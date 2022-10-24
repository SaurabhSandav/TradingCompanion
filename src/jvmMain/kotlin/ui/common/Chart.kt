package ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import chart.IChartApi

@Composable
fun ResizableChart(
    chart: IChartApi,
    onChartLoaded: IChartApi.() -> Unit,
) {

    Column(Modifier.fillMaxSize()) {

        val webViewState = rememberWebViewState()
        var initialSize by state { IntSize.Zero }

        WebViewLoadingIndicator(webViewState)

        JavaFxWebView(
            state = webViewState,
            modifier = Modifier.fillMaxSize().onSizeChanged { size ->

                initialSize = size

                // Resize chart on layout resize
                if (chart.isInitialized) chart.resize(
                    width = size.width,
                    height = size.height,
                )
            },
        )

        if (webViewState.isReady) {

            // Configure chart if WebView is ready
            LaunchedEffect(Unit) {

                // Load chart webpage
                webViewState.load(
                    WebViewState::class.java
                        .getResource("/charts_page/index.html")!!
                        .toExternalForm()
                )

                // On page load, execute chart script
                webViewState.loadState.collect { loadState ->

                    if (loadState != WebViewState.LoadState.LOADED) return@collect

                    chart.init(
                        container = "document.body",
                        executeJs = webViewState::executeScript,
                    )

                    chart.onChartLoaded()

                    // Initial resize in case onSizeChanged isn't called after init()
                    chart.resize(
                        width = initialSize.width,
                        height = initialSize.height,
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
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    LaunchedEffect(webViewState.loadState) {
        webViewState.loadState.collect {
            isLoading = it != WebViewState.LoadState.LOADED
        }
    }
}
