package com.saurabhsandav.core.ui.common.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.saurabhsandav.core.ui.common.JavaFxWebView
import com.saurabhsandav.core.ui.common.WebViewState
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.state

@Composable
fun ChartPage(
    state: ChartPageState,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {

        WebViewLoadingIndicator(state.webViewState)

        JavaFxWebView(
            state = state.webViewState,
            modifier = Modifier.fillMaxSize().onSizeChanged { size ->

                // Resize chart on layout resize
                state.resize(size)
            },
        )

        val backgroundColor = MaterialTheme.colorScheme.background

        // Set Material background as page background
        LaunchedEffect(backgroundColor) {
            state.webViewState.setBackgroundColor(backgroundColor)
            state.setPageBackgroundColor(backgroundColor)
        }

        val textColor = LocalContentColor.current

        // Set Material text color on chart text
        LaunchedEffect(textColor) {
            state.setLegendTextColor(textColor)
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
