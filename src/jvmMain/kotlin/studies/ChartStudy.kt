package studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chart.Chart
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import ui.common.state

internal abstract class ChartStudy : Study {

    @Composable
    final override fun render() {

        render {

            Column(Modifier.fillMaxSize()) {

                var isLoading by state { true }
                val coroutineScope = rememberCoroutineScope()
                var chart by state<Chart?> { null }
                var initialSize by state<IntSize?> { null }

                if (isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 2.dp))
                }

                SwingPanel(
                    modifier = Modifier.fillMaxSize(0.8F).onSizeChanged {
                        initialSize = it
                        chart?.resize(
                            width = (it.width * 1.2).toInt(),
                            height = (it.height * 1.2).toInt(),
                        )
                    },
                    factory = {

                        JFXPanel().apply {

                            Platform.runLater {
                                Platform.setImplicitExit(false)
                                val webView = WebView()
                                val engine = webView.engine
                                val url = this@ChartStudy::class.java
                                    .getResource("/charts_page/index.html")!!
                                    .toExternalForm()
                                engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                                    when (newValue) {
                                        Worker.State.SUCCEEDED -> {
                                            isLoading = false
                                            chart = Chart(engine)
                                            chart!!.resize(
                                                width = (initialSize!!.width * 1.2).toInt(),
                                                height = (initialSize!!.height * 1.2).toInt(),
                                            )
                                            coroutineScope.configureChart(chart!!)
                                        }

                                        else -> {}
                                    }
                                }
                                engine.load(url)
                                val wvScene = Scene(webView)
                                scene = wvScene
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    protected open fun render(chart: @Composable () -> Unit) {
        chart()
    }

    protected abstract fun CoroutineScope.configureChart(chart: Chart)
}
