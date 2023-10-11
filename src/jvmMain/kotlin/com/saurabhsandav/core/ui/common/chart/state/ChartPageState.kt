package com.saurabhsandav.core.ui.common.chart.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.toHexString
import com.saurabhsandav.core.ui.common.webview.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Stable
fun ChartPageState(
    coroutineScope: CoroutineScope,
    webView: WebView,
    chart: IChartApi,
) = ChartPageState(
    coroutineScope = coroutineScope,
    arrangement = ChartArrangement.single(),
    webView = webView,
).apply { connect(chart) }

@Stable
class ChartPageState(
    private val coroutineScope: CoroutineScope,
    private val arrangement: ChartArrangement,
    val webView: WebView,
) {

    private val scripts = Channel<String>(Channel.UNLIMITED)
    private val charts = mutableListOf<IChartApi>()

    init {

        coroutineScope.launch {

            // Wait for WebView initialization
            webView.awaitReady()

            // Start server for serving chart page
            ChartsPageServer.startIfNotStarted()

            // Load chart webpage
            webView.load(ChartsPageServer.getUrl())

            // Await page load
            webView.loadState.first { loadState -> loadState == WebView.LoadState.LOADED }

            // Forward callbacks
            webView.createJSCallback("chartCallback")
                .messages
                .onEach(::onCallback)
                .launchIn(coroutineScope)

            // Execute chart scripts
            merge(arrangement.scripts, scripts.consumeAsFlow()).onEach(webView::executeScript).collect()
        }
    }

    private fun onCallback(message: String) {
        // If arrangement does not consume callback, forward it to the charts
        if (!arrangement.onCallback(message))
            charts.forEach { it.onCallback(message) }
    }

    fun setPageBackgroundColor(color: Color) {
        scripts.trySend("setPageBackgroundColor('${color.toHexString()}');")
    }

    fun setLegendTextColor(color: Color) {
        scripts.trySend("setLegendTextColor('${color.toHexString()}');")
    }

    fun connect(chart: IChartApi) {

        // Cache chart
        charts.add(chart)

        // Send chart js scripts to web engine
        coroutineScope.launch {
            chart.scripts.collect(scripts::trySend)
        }
    }

    fun disconnect(chart: IChartApi) {
        charts.remove(chart)
    }
}
