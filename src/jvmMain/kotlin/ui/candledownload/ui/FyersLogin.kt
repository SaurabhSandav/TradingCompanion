package ui.candledownload.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import ui.common.state

@Composable
internal fun FyersLoginWebView(
    url: String,
    onLoginSuccess: (redirectUrl: String) -> Unit,
) {

    Column(Modifier.fillMaxSize()) {

        var isLoading by state { true }

        if (isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 2.dp))
        }

        SwingPanel(modifier = Modifier.fillMaxSize(0.8F), factory = {

            JFXPanel().apply {

                Platform.runLater {
                    Platform.setImplicitExit(false)
                    val webView = WebView()
                    val engine = webView.engine
                    engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                        when (newValue) {
                            Worker.State.SUCCEEDED -> isLoading = false
                            else -> {}
                        }
                    }
                    engine.locationProperty().addListener { _, _, newLocation ->
                        if (newLocation != null && newLocation.startsWith("http://localhost:8080")) {
                            onLoginSuccess(newLocation)
                        }
                    }
                    engine.load(url)
                    val wvScene = Scene(webView)
                    scene = wvScene
                }
            }
        })
    }
}
