package ui.common.chart

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
import ui.common.JavaFxWebView
import ui.common.WebViewState
import ui.common.chart.state.ChartState
import ui.common.state

@Composable
fun ChartPage(
    state: ChartState,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {

        WebViewLoadingIndicator(state.webViewState)

        JavaFxWebView(
            state = state.webViewState,
            modifier = Modifier.fillMaxSize().onSizeChanged { size ->

                // Resize chart on layout resize
                state.resize(
                    width = size.width,
                    height = size.height,
                )
            },
        )
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
