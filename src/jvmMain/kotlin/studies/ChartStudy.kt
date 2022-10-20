package studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chart.ChartOptions
import chart.IChartApi
import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import ui.candledownload.CandleDownloadPresenter
import ui.common.JavaFxWebView
import ui.common.WebViewState
import ui.common.rememberWebViewState
import ui.common.state

internal abstract class ChartStudy : Study {

    @Composable
    final override fun render() {

        render {

            Column(Modifier.fillMaxSize()) {

                val webViewState = rememberWebViewState()
                val coroutineScope = rememberCoroutineScope()
                var chart by state<IChartApi?> { null }
                var size by state { IntSize.Zero }

                WebViewLoadingIndicator(webViewState)

                JavaFxWebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize(0.8F).onSizeChanged { size = it },
                )

                // Configure chart if WebView is ready
                LaunchedEffect(webViewState.isReady) {

                    // Return if WebView not ready
                    if (!webViewState.isReady) return@LaunchedEffect

                    // Load chart webpage
                    webViewState.load(
                        CandleDownloadPresenter::class.java
                            .getResource("/charts_page/index.html")!!
                            .toExternalForm()
                    )

                    // On page load, execute chart script
                    webViewState.loadState.collect {

                        if (it != WebViewState.LoadState.LOADED) return@collect

                        chart = IChartApi(
                            executeJs = {
                                Platform.runLater {
                                    webViewState.executeScript(it)
                                }
                            },
                            options = ChartOptions(
                                width = (size.width * 1.2).toInt(),
                                height = (size.height * 1.2).toInt(),
                            ),
                        )

                        coroutineScope.configureChart(chart!!)
                    }
                }

                // Resize chart on window resize
                LaunchedEffect(Unit) {
                    snapshotFlow { size }.collect {
                        chart?.resize(
                            width = (size.width * 1.2).toInt(),
                            height = (size.height * 1.2).toInt(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WebViewLoadingIndicator(webViewState: WebViewState) {

        var isLoading by state { true }

        if (isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 2.dp))
        }

        LaunchedEffect(webViewState.loadState) {
            webViewState.loadState.collect {
                isLoading = it != WebViewState.LoadState.LOADED
            }
        }
    }

    @Composable
    protected open fun render(chart: @Composable () -> Unit) {
        chart()
    }

    protected abstract fun CoroutineScope.configureChart(chart: IChartApi)
}
