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
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toAwtColor
import com.saurabhsandav.core.ui.common.webview.WebView

@Composable
fun ChartPage(
    state: ChartPageState,
    modifier: Modifier = Modifier,
) {

    Column(modifier) {

        WebViewLoadingIndicator(state.webView)

        WebView(
            webView = state.webView,
            modifier = Modifier.fillMaxSize(),
        )

        val backgroundColor = MaterialTheme.colorScheme.background

        // Set Material background as page background
        LaunchedEffect(backgroundColor) {
            state.webView.setBackgroundColor(backgroundColor.toAwtColor())
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
private fun WebViewLoadingIndicator(webView: WebView) {

    var isLoading by state { true }

    if (isLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    LaunchedEffect(webView.loadState) {
        webView.loadState.collect {
            isLoading = it != WebView.LoadState.LOADED
        }
    }
}
