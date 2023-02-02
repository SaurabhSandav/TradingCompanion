package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.app.AppSwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import netscape.javascript.JSObject

@Composable
fun JavaFxWebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
) {

    AppSwingPanel(
        modifier = modifier,
        factory = { state.jfxPanel }
    )
}

@Composable
fun rememberWebViewState(): WebViewState {

    val coroutineScope = rememberCoroutineScope()

    return remember(coroutineScope) { WebViewState(coroutineScope) }
}

@Stable
class WebViewState(
    private val coroutineScope: CoroutineScope,
    private val isFocusable: Boolean = true,
) {

    private lateinit var webView: WebView
    private lateinit var engine: WebEngine
    private var backgroundColor: Color? = null

    var isReady by mutableStateOf(false)

    private val _loadState = MutableSharedFlow<LoadState>(
        replay = 1,
        extraBufferCapacity = 10,
    )
    val loadState: Flow<LoadState> = _loadState.distinctUntilChanged()

    private val _location = MutableSharedFlow<String>(replay = 1)
    val location: Flow<String> = _location.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
    val errors: Flow<Throwable> = _errors.asSharedFlow()

    internal val jfxPanel = JFXPanel().apply {

        Platform.runLater {

            Platform.setImplicitExit(false)

            isFocusable = this@WebViewState.isFocusable

            webView = WebView()

            setEngine(webView.engine)

            scene = Scene(webView)
        }
    }

    fun load(url: String) {

        requireReady()

        Platform.runLater {
            engine.load(url)
        }
    }

    fun executeScript(script: String) {

        requireReady()

        Platform.runLater {
            engine.executeScript(script)
        }
    }

    fun setMember(name: String, memberObject: Any) {

        requireReady()

        Platform.runLater {
            val window = engine.executeScript("window") as JSObject
            window.setMember(name, memberObject)
        }
    }

    fun setBackgroundColor(color: androidx.compose.ui.graphics.Color) {

        backgroundColor = Color.web(color.toHexString())

        if (isReady) {
            Platform.runLater {
                webView.pageFill = backgroundColor
            }
        }
    }

    private fun setEngine(webEngine: WebEngine) {

        engine = webEngine

        // Page background color
        backgroundColor?.let { webView.pageFill = it }

        engine.loadWorker.stateProperty().addListener { _, _, newValue ->

            newValue ?: return@addListener

            val loadState = when (newValue) {
                Worker.State.READY -> LoadState.INITIALIZED
                Worker.State.SCHEDULED, Worker.State.RUNNING -> LoadState.LOADING
                Worker.State.SUCCEEDED -> LoadState.LOADED
                Worker.State.CANCELLED, Worker.State.FAILED -> LoadState.FAILED
            }

            if (!_loadState.tryEmit(loadState)) error("Could not emit WebView load state")
        }

        engine.loadWorker.exceptionProperty().addListener { _, _, newValue ->
            newValue ?: return@addListener
            if (!_errors.tryEmit(newValue)) error("Could not emit WebView errors")
        }

        engine.locationProperty().addListener { _, _, newValue ->
            newValue ?: return@addListener
            if (!_location.tryEmit(newValue)) error("Could not emit WebView location")
        }

        coroutineScope.launch {
            isReady = true
        }
    }

    private fun requireReady() {
        if (!::engine.isInitialized) error("WebView is not ready. Check isReady before any operation")
    }

    enum class LoadState {
        INITIALIZED,
        LOADING,
        LOADED,
        FAILED;
    }
}
