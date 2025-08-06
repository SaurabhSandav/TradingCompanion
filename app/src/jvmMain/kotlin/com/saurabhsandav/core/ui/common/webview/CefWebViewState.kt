package com.saurabhsandav.core.ui.common.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.jetbrains.cef.JCefAppConfig
import com.saurabhsandav.core.LocalAppConfig
import com.saurabhsandav.core.originalDensity
import com.saurabhsandav.core.ui.common.AwtColor
import com.saurabhsandav.core.ui.common.webview.WebViewState.LoadState
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.libs.jcefcompose.ComposeCefOSRBrowser
import com.saurabhsandav.libs.jcefcompose.WebView
import com.saurabhsandav.libs.jcefcompose.createComposeOffScreenBrowser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.cef.CefApp
import org.cef.CefApp.CefAppState
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefAppHandlerAdapter
import org.cef.handler.CefContextMenuHandlerAdapter
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.network.CefRequest
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

class CefWebViewState(
    private val coroutineScope: CoroutineScope,
    private val appDispatchers: AppDispatchers,
    private val myCefApp: MyCefApp,
) : WebViewState {

    private val initMutex = Mutex()
    private val browserProps = BrowserProps()

    @Suppress("ktlint:standard:backing-property-naming")
    private var _browser: CefBrowser? = null
    private val browser: CefBrowser
        get() = checkNotNull(_browser) { "Browser not initialized" }

    override val loadState: Flow<LoadState> = browserProps.mutableLoadState.asStateFlow()
    override val location: Flow<String> = browserProps.mutableLocation.asStateFlow()
    override val errors: Flow<Throwable> = browserProps.mutableErrors.asSharedFlow()

    private val isCreatedAndReady
        get() = browserProps.run { isCreated.isCompleted && isReady.isCompleted }

    @Composable
    override fun WebView(modifier: Modifier) {

        val isReady by produceState(isCreatedAndReady) {
            init()
            value = true
        }

        when {
            isReady -> {

                val appConfig = LocalAppConfig.current

                CompositionLocalProvider(LocalDensity provides appConfig.originalDensity()) {

                    WebView(
                        modifier = modifier,
                        browser = browser as ComposeCefOSRBrowser,
                    )
                }
            }

            else -> CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
        }
    }

    override suspend fun init() = initMutex.withLock {

        if (_browser != null) return@withLock

        _browser = withContext(appDispatchers.IO) { myCefApp.createBrowser(browserProps) }

        browserProps.isReady.complete(Unit)

        coroutineScope.launch {

            try {
                awaitCancellation()
            } finally {
                myCefApp.closeBrowser(browser)
                _browser = null
            }
        }
    }

    override suspend fun awaitReady() {
        browserProps.isCreated.await()
        browserProps.isReady.await()
    }

    override suspend fun load(url: String) {

        awaitReady()

        browser.loadURL(url)
    }

    override suspend fun executeScript(script: String) {

        awaitReady()

        browser.executeJavaScript(script, null, 0)
    }

    override suspend fun createJSCallback(jsFuncName: String): WebViewState.JSCallback {

        awaitReady()

        val flow = MutableSharedFlow<String>(extraBufferCapacity = Channel.UNLIMITED)

        val script = """|
            |function $jsFuncName(callbackMessage) {
            |  window.cefQuery({
            |      request: JSON.stringify({ id: "$jsFuncName", message: callbackMessage }),
            |      persistent: false,
            |      onSuccess: function(response) {},
            |      onFailure: function(error_code, error_message) {}
            |  });
            |}
        """.trimMargin()

        browser.executeJavaScript(script, null, 0)

        val callback = object : CefJSCallback(flow) {

            override suspend fun remove() {

                browserProps.jsCallbacks.remove(jsFuncName)

                browser.executeJavaScript("$jsFuncName = function(){}", null, 0)
            }
        }

        browserProps.jsCallbacks[jsFuncName] = callback

        return callback
    }

    override suspend fun setBackgroundColor(color: AwtColor) {
        // Not supported
    }
}

class MyCefApp(
    private val appPaths: AppPaths,
) {

    private val mutex = Mutex()
    private var client: CefClient? = null
    private val browserPropsMap = mutableMapOf<CefBrowser, BrowserProps>()

    private suspend fun getInstance(): CefApp {

        CefApp.getInstanceIfAny()?.let { return it }

        CefApp.startup(emptyArray())

        val jCefAppConfig = JCefAppConfig.getInstance()

        val contextInitialized = CompletableDeferred<Unit>()

        CefApp.addAppHandler(
            object : CefAppHandlerAdapter(jCefAppConfig.appArgs) {
                override fun stateHasChanged(state: CefAppState) {
                    // Shutdown the app if the native CEF part is terminated
                    if (state == CefAppState.TERMINATED) exitProcess(0)
                }

                override fun onContextInitialized() {
                    contextInitialized.complete(Unit)
                }
            },
        )

        val cefSettings = jCefAppConfig.cefSettings.apply {
            cache_path = appPaths.appDataPath.resolve("CEF").absolutePathString()
        }

        val instance = CefApp.getInstance(cefSettings)

        contextInitialized.await()

        return instance
    }

    private suspend fun getClient(): CefClient {

        client?.let { return it }

        val client = getInstance().createClient().apply {

            val msgRouter = CefMessageRouter.create(
                object : CefMessageRouterHandlerAdapter() {
                    override fun onQuery(
                        browser: CefBrowser,
                        frame: CefFrame?,
                        queryId: Long,
                        request: String,
                        persistent: Boolean,
                        callback: CefQueryCallback,
                    ): Boolean {

                        val jsonElement = Json.parseToJsonElement(request)

                        val id = jsonElement.jsonObject["id"]!!.jsonPrimitive.content
                        val message = jsonElement.jsonObject["message"]!!.jsonPrimitive.content

                        browserPropsMap[browser]?.jsCallbacks?.get(id)?.mutableSharedFlow?.tryEmit(message)

                        callback.success("")
                        return true
                    }
                },
            )

            addMessageRouter(msgRouter)
        }

        client.addLifeSpanHandler(
            object : CefLifeSpanHandlerAdapter() {
                override fun onAfterCreated(browser: CefBrowser) {
                    browserPropsMap[browser]?.isCreated?.complete(Unit)
                }
            },
        )

        // Disable context menu
        client.addContextMenuHandler(
            object : CefContextMenuHandlerAdapter() {

                override fun onBeforeContextMenu(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    params: CefContextMenuParams?,
                    model: CefMenuModel,
                ) {
                    model.clear()
                }
            },
        )

        // URL
        client.addDisplayHandler(
            object : CefDisplayHandlerAdapter() {

                override fun onAddressChange(
                    browser: CefBrowser,
                    frame: CefFrame?,
                    url: String,
                ) {
                    super.onAddressChange(browser, frame, url)
                    browserPropsMap[browser]?.mutableLocation?.value = url
                }
            },
        )

        client.addLoadHandler(
            object : CefLoadHandlerAdapter() {

                override fun onLoadStart(
                    browser: CefBrowser,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?,
                ) {
                    emitLoadState(browser, LoadState.LOADING)
                }

                override fun onLoadEnd(
                    browser: CefBrowser,
                    frame: CefFrame?,
                    httpStatusCode: Int,
                ) {
                    emitLoadState(browser, LoadState.LOADED)
                }

                override fun onLoadError(
                    browser: CefBrowser,
                    frame: CefFrame?,
                    errorCode: CefLoadHandler.ErrorCode?,
                    errorText: String?,
                    failedUrl: String?,
                ) {
                    emitLoadState(browser, LoadState.FAILED)
                    browserPropsMap[browser]?.mutableErrors?.tryEmit(Throwable(errorText))
                }

                private fun emitLoadState(
                    browser: CefBrowser,
                    loadState: LoadState,
                ) {
                    browserPropsMap[browser]?.mutableLoadState?.tryEmit(loadState)
                }
            },
        )

        this.client = client

        return client
    }

    internal suspend fun createBrowser(browserProps: BrowserProps): CefBrowser {

        val client = mutex.withLock { getClient() }
        val browser = client.createComposeOffScreenBrowser()

        browserPropsMap[browser] = browserProps

        return browser
    }

    fun closeBrowser(browser: CefBrowser) {
        browser.close(false)
        browserPropsMap.remove(browser)

        // If all browsers are closed, dispose client.
        if (browserPropsMap.isEmpty()) disposeClient()
    }

    private fun disposeClient() {
        client?.dispose()
        client = null
    }

    fun dispose() = CefApp.getInstanceIfAny()?.dispose()
}

internal class BrowserProps {

    val jsCallbacks = mutableMapOf<String, CefJSCallback>()

    val isCreated = CompletableDeferred<Unit>()

    val isReady = CompletableDeferred<Unit>()

    val mutableLoadState = MutableStateFlow(LoadState.INITIALIZED)

    val mutableLocation = MutableStateFlow("")

    val mutableErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
}

internal abstract class CefJSCallback(
    val mutableSharedFlow: MutableSharedFlow<String>,
) : WebViewState.JSCallback {

    override val messages: Flow<String> = mutableSharedFlow.asSharedFlow()
}
