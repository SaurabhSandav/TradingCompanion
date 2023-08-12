package com.saurabhsandav.core.ui.common.webview

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.toHexString
import com.saurabhsandav.core.ui.common.webview.WebView.LoadState
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.web.WebEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import netscape.javascript.JSObject
import javafx.scene.web.WebView as JFXWebView


@Stable
class JavaFxWebView : WebView {

    private lateinit var webView: JFXWebView
    private lateinit var engine: WebEngine
    private var backgroundColor: Color? = null

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

    override val component = JFXPanel()

    override suspend fun awaitReady() = isReady.await()

    override suspend fun init() {

        check(!isReady.isCompleted) { "WebView already initialized" }

        runInJavaFxThread {

            Platform.setImplicitExit(false)

            component.isFocusable = true

            webView = JFXWebView()

            setEngine(webView.engine)

            component.scene = Scene(webView)
        }
    }

    override suspend fun load(url: String) {

        awaitReady()

        runInJavaFxThread { engine.load(url) }
    }

    override suspend fun loadResource(path: String) {

        awaitReady()

        val url = JFXWebView::class.java.getResource(path)?.toExternalForm() ?: error("Resource '$path' not found")

        runInJavaFxThread { engine.load(url) }
    }

    override suspend fun executeScript(script: String) {

        awaitReady()

        runInJavaFxThread { engine.executeScript(script) }
    }

    override suspend fun createJSCallback(jsFuncName: String): WebView.JSCallback {

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

        return object : WebView.JSCallback {

            override val messages: Flow<String> = flow.asSharedFlow()

            override suspend fun remove() = runInJavaFxThread {

                val window = engine.executeScript("window") as JSObject

                window.removeMember(callbackRef)

                engine.executeScript("$jsFuncName = function(){}")
            }
        }
    }

    override suspend fun setBackgroundColor(color: androidx.compose.ui.graphics.Color) {

        awaitReady()

        runInJavaFxThread {

            backgroundColor = Color.web(color.toHexString())

            webView.pageFill = backgroundColor
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

        isReady.complete(Unit)
    }

    private fun runInJavaFxThread(block: () -> Unit) = Platform.runLater(block)
}
