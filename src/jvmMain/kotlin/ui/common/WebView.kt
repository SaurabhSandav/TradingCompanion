package ui.common

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Composable
fun JavaFxWebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
) {

    val jfxPanel = remember {
        JFXPanel().apply {

            Platform.runLater {

                Platform.setImplicitExit(false)

                val webView = WebView()

                state.setEngine(webView.engine)

                scene = Scene(webView)
            }
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = { jfxPanel }
    )
}

@Composable
fun rememberWebViewState(): WebViewState {

    val coroutineScope = rememberCoroutineScope()

    return remember(coroutineScope) { WebViewState(coroutineScope) }
}

class WebViewState(
    private val coroutineScope: CoroutineScope,
) {

    private lateinit var engine: WebEngine

    var isReady by mutableStateOf(false)

    private val _loadState = MutableSharedFlow<LoadState>(extraBufferCapacity = 10)
    val loadState: Flow<LoadState> = _loadState.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
    val errors: Flow<Throwable> = _errors.asSharedFlow()

    internal fun setEngine(webEngine: WebEngine) {

        engine = webEngine

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
            if (!_errors.tryEmit(newValue)) error("Could not emit WebView errors")
        }

        coroutineScope.launch {
            isReady = true
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
