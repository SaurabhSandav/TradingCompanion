package ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import chart.IChartApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun ResizableChart(
    state: ChartState,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {

        var initialSize by state { IntSize.Zero }

        WebViewLoadingIndicator(state.webViewState)

        JavaFxWebView(
            state = state.webViewState,
            modifier = Modifier.fillMaxSize().onSizeChanged { size ->

                initialSize = size

                // Resize chart on layout resize
                state.chart.resize(
                    width = size.width,
                    height = size.height,
                )
            },
        )
    }
}

@Composable
fun rememberChartState(chart: IChartApi): ChartState {
    val coroutineScope = rememberCoroutineScope()
    return remember { ChartState(coroutineScope, chart) }
}

class ChartState(
    coroutineScope: CoroutineScope,
    val chart: IChartApi,
) {

    val webViewState = WebViewState(coroutineScope)

    init {
        coroutineScope.launch {

            // Configure chart if WebView is ready
            snapshotFlow { webViewState.isReady }.filter { it }.collect {

                // Load chart webpage
                webViewState.load(
                    WebViewState::class.java
                        .getResource("/charts_page/index.html")!!
                        .toExternalForm()
                )

                // On page load, execute chart scripts
                webViewState.loadState.collect { loadState ->
                    if (loadState == WebViewState.LoadState.LOADED) {
                        webViewState.setMember(chart.javaCallbacksObjectName, chart.javaCallbacks)
                        chart.scripts.collect(webViewState::executeScript)
                    }
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
