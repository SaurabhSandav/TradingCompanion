package com.saurabhsandav.core.ui.common.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toAwtColor
import com.saurabhsandav.core.ui.common.webview.WebView
import com.saurabhsandav.core.ui.common.webview.WebViewState

@Composable
fun ChartPage(
    state: ChartPageState,
    modifier: Modifier = Modifier,
    legend: (@Composable () -> Unit)? = null,
) {

    Column(modifier) {

        WebViewLoadingIndicator(state.webViewState)

        Box(Modifier.fillMaxSize()) {

            WebView(
                webViewState = state.webViewState,
                modifier = Modifier.fillMaxSize(),
            )

            if (legend != null) {

                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    legend()
                }
            }
        }

        val backgroundColor = MaterialTheme.colorScheme.background

        // Set Material background as page background
        LaunchedEffect(backgroundColor) {
            state.webViewState.setBackgroundColor(backgroundColor.toAwtColor())
            state.setPageBackgroundColor(backgroundColor)
        }
    }
}

@Composable
private fun WebViewLoadingIndicator(webViewState: WebViewState) {

    var isLoading by state { false }

    if (isLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    LaunchedEffect(webViewState.loadState) {
        webViewState.loadState.collect {
            isLoading = it != WebViewState.LoadState.LOADED
        }
    }
}
