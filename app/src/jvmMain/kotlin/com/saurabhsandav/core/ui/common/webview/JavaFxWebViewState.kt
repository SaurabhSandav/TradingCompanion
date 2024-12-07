package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AwtColor
import com.saurabhsandav.core.ui.common.JFXColor
import com.saurabhsandav.core.ui.common.app.AppSwingPanel
import com.saurabhsandav.core.ui.common.toJavaFxColor
import com.saurabhsandav.core.ui.common.webview.WebViewState.LoadState
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import netscape.javascript.JSObject
import javafx.scene.web.WebView as JFXWebView

class JavaFxWebViewState : WebViewState {

    private lateinit var webView: JFXWebView
    private val engine: WebEngine
        get() = webView.engine
    private var backgroundColor: JFXColor? = null

    private val isReady = CompletableDeferred<Unit>()

    private val _loadState = MutableStateFlow(LoadState.INITIALIZED)
    override val loadState: Flow<LoadState> = _loadState.asStateFlow()

    private val _location = MutableStateFlow("")
    override val location: Flow<String> = _location.asStateFlow()

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
    override val errors: Flow<Throwable> = _errors.asSharedFlow()

    private val component = JFXPanel()

    @Composable
    override fun WebView(modifier: Modifier) {

        AppSwingPanel(
            modifier = modifier,
            factory = { component }
        )

        LaunchedEffect(Unit) {

            if (::webView.isInitialized) return@LaunchedEffect

            runInJavaFxThread {

                Platform.setImplicitExit(false)

                component.isFocusable = false

                webView = JFXWebView()

                setEngine()

                webView.isContextMenuEnabled = false

                component.scene = Scene(webView)
            }
        }
    }

    override suspend fun awaitReady() = isReady.await()

    override suspend fun load(url: String) {

        awaitReady()

        runInJavaFxThread {

            _loadState.value = LoadState.INITIALIZED

            engine.load(url)
        }
    }

    override suspend fun executeScript(script: String) {

        awaitReady()

        runInJavaFxThread { engine.executeScript(script) }
    }

    override suspend fun createJSCallback(jsFuncName: String): WebViewState.JSCallback {

        awaitReady()

        val callbackRef = "${jsFuncName}Flow"
        val flow = MutableSharedFlow<String>(extraBufferCapacity = Channel.UNLIMITED)

        runInJavaFxThread {

            engine.executeScript("""|
                |function $jsFuncName(callbackMessage) {
                |  $callbackRef.tryEmit(callbackMessage)
                |}""".trimMargin()
            )

            val window = engine.executeScript("window") as JSObject
            window.setMember(callbackRef, flow)
        }

        return object : WebViewState.JSCallback {

            override val messages: Flow<String> = flow.asSharedFlow()

            override suspend fun remove() = runInJavaFxThread {

                val window = engine.executeScript("window") as JSObject

                window.removeMember(callbackRef)

                engine.executeScript("$jsFuncName = function(){}")
            }
        }
    }

    override suspend fun setBackgroundColor(color: AwtColor) {

        awaitReady()

        runInJavaFxThread {

            backgroundColor = color.toJavaFxColor()

            webView.pageFill = backgroundColor
        }
    }

    private fun setEngine() {

        // Page background color
        backgroundColor?.let { webView.pageFill = it }

        engine.loadWorker.stateProperty().addListener { _, _, newValue ->

            newValue ?: return@addListener

            // Sometimes after a SUCCEEDED emission loadWorker re-emits SCHEDULED, RUNNING but not SUCCEEDED.
            // Could not find out the cause for this.
            // Work around it by ignoring SCHEDULED, RUNNING emissions when new load was not explicitly requested.
            // Track explicit load requests by setting loadState to INITIALIZED.

            val loadState = when (newValue) {
                Worker.State.READY -> LoadState.INITIALIZED
                Worker.State.SCHEDULED, Worker.State.RUNNING -> when (_loadState.value) {
                    LoadState.INITIALIZED -> LoadState.LOADING
                    else -> _loadState.value
                }
                Worker.State.SUCCEEDED -> LoadState.LOADED
                Worker.State.CANCELLED, Worker.State.FAILED -> LoadState.FAILED
            }

            _loadState.value = loadState
        }

        engine.loadWorker.exceptionProperty().addListener { _, _, newValue ->
            newValue ?: return@addListener
            if (!_errors.tryEmit(newValue)) error("Could not emit WebView errors")
        }

        engine.locationProperty().addListener { _, _, newValue ->
            _location.value = newValue ?: return@addListener
        }

        isReady.complete(Unit)
    }

    private fun runInJavaFxThread(block: () -> Unit) = Platform.runLater(block)
}
