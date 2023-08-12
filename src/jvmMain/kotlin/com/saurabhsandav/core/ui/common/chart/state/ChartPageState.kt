package com.saurabhsandav.core.ui.common.chart.state

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
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
    private var currentSize = IntSize.Zero

    init {

        coroutineScope.launch {

            // Wait for WebView initialization
            webView.awaitReady()

            // Load chart webpage
            webView.loadResource("/charts_page/index.html")

            // Await page load
            webView.loadState.first { loadState -> loadState == WebView.LoadState.LOADED }

            // Forward callbacks
            webView.createJSCallback("chartCallback")
                .messages
                .onEach(::onCallback)
                .launchIn(coroutineScope)

            // Execute chart scripts
            scripts.consumeAsFlow().onEach(webView::executeScript).collect()
        }

        // Send arrangement js scripts to web engine
        coroutineScope.launch {
            arrangement.scripts.collect(scripts::trySend)
        }
    }

    private fun onCallback(message: String) {
        // If arrangement does not consume callback, forward it to the charts
        if (!arrangement.onCallback(message))
            charts.forEach { it.onCallback(message) }
    }

    fun resize(size: IntSize) {

        // Resize only if necessary
        if (size == currentSize) return

        currentSize = size

        // Resize all charts
        charts.forEach { it.resize(width = size.width, height = size.height) }
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

        // Set initial size for chart
        chart.resize(width = currentSize.width, height = currentSize.height)

        // Send chart js scripts to web engine
        coroutineScope.launch {
            chart.scripts.collect(scripts::trySend)
        }
    }

    fun disconnect(chart: IChartApi) {
        charts.remove(chart)
    }
}
