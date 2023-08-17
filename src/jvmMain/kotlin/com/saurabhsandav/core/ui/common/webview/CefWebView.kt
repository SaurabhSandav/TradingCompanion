package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.AwtColor
import com.saurabhsandav.core.ui.common.webview.WebView.LoadState
import com.saurabhsandav.core.utils.AppPaths
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.OS
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.callback.CefQueryCallback
import org.cef.handler.*
import org.cef.network.CefRequest
import java.awt.Component
import java.io.File

@Stable
class CefWebView : WebView {

    private val jsCallbacks = mutableMapOf<String, CefJSCallback>()

    private val client = CefApp.createClient().apply {

        val msgRouter = CefMessageRouter.create(object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(
                browser: CefBrowser?,
                frame: CefFrame?,
                queryId: Long,
                request: String,
                persistent: Boolean,
                callback: CefQueryCallback,
            ): Boolean {

                val jsonElement = Json.parseToJsonElement(request)

                val id = jsonElement.jsonObject["id"]!!.jsonPrimitive.content
                val message = jsonElement.jsonObject["message"]!!.jsonPrimitive.content

                jsCallbacks[id]?.mutableSharedFlow?.tryEmit(message)

                callback.success("")
                return true
            }
        })

        addMessageRouter(msgRouter)
    }

    private val browser = client.createBrowser(
        /* url = */ null,
        /* isOffscreenRendered = */ OS.isLinux(),
        /* isTransparent = */ true,
    )

    private val isReady = CompletableDeferred<Unit>()

    private val _loadState = MutableSharedFlow<LoadState>(
        replay = 1,
        extraBufferCapacity = 10,
    )
    override val loadState: Flow<LoadState> = _loadState.distinctUntilChanged()

    private val _location = MutableSharedFlow<String>(replay = 1)
    override val location: Flow<String> = _location.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
    override val errors: Flow<Throwable> = _errors.asSharedFlow()

    override val component: Component = browser.uiComponent

    override suspend fun awaitReady() = isReady.await()

    override suspend fun init() {

        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser?) {
                isReady.complete(Unit)
            }
        })

        // Disable context menu
        client.addContextMenuHandler(object : CefContextMenuHandlerAdapter() {

            override fun onBeforeContextMenu(
                browser: CefBrowser?,
                frame: CefFrame?,
                params: CefContextMenuParams?,
                model: CefMenuModel,
            ) {
                model.clear()
            }
        })

        client.addLoadHandler(object : CefLoadHandlerAdapter() {

            init {
                emitLoadState(LoadState.INITIALIZED)
            }

            override fun onLoadStart(
                browser: CefBrowser?,
                frame: CefFrame?,
                transitionType: CefRequest.TransitionType?,
            ) {
                emitLoadState(LoadState.LOADING)
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame?, httpStatusCode: Int) {
                emitLoadState(LoadState.LOADED)
                if (!_location.tryEmit(browser.url)) error("Could not emit WebView location")
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?,
            ) {
                emitLoadState(LoadState.FAILED)
                if (!_errors.tryEmit(Throwable(errorText))) error("Could not emit WebView errors")
            }

            private fun emitLoadState(loadState: LoadState) {
                if (!_loadState.tryEmit(loadState)) error("Could not emit WebView load state")
            }
        })
    }

    override suspend fun load(url: String) {

        awaitReady()

        browser.loadURL(url)
    }

    override suspend fun executeScript(script: String) {

        awaitReady()

        browser.executeJavaScript(script, null, 0)
    }

    override suspend fun createJSCallback(jsFuncName: String): WebView.JSCallback {

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

                jsCallbacks.remove(jsFuncName)

                browser.executeJavaScript("$jsFuncName = function(){}", null, 0)
            }
        }

        jsCallbacks[jsFuncName] = callback

        return callback
    }

    override suspend fun setBackgroundColor(color: AwtColor) {
        // Not supported
    }

    private abstract class CefJSCallback(
        val mutableSharedFlow: MutableSharedFlow<String>,
    ) : WebView.JSCallback {

        override val messages: Flow<String> = mutableSharedFlow.asSharedFlow()
    }
}

val CefApp: CefApp = CefAppBuilder().run {
    setInstallDir(File(AppPaths.getAppDataPath() + "/jcef-bundle"))
    build()
}
