package com.saurabhsandav.core.ui.common.chart.state

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.single
import com.saurabhsandav.core.ui.common.toHexString
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.lightweight_charts.IChartApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun ChartPageState(
    coroutineScope: CoroutineScope,
    webViewState: WebViewState,
    chart: IChartApi,
) = ChartPageState(
    coroutineScope = coroutineScope,
    arrangement = ChartArrangement.single(),
    webViewState = webViewState,
).apply { connect(chart) }

class ChartPageState(
    private val coroutineScope: CoroutineScope,
    private val arrangement: ChartArrangement,
    val webViewState: WebViewState,
) {

    private val scripts = Channel<String>(Channel.UNLIMITED)
    private val charts = mutableListOf<IChartApi>()

    init {

        coroutineScope.launch {

            coroutineScope {

                // Wait for WebView initialization
                launch { webViewState.awaitReady() }

                // Start server for serving chart page
                launch { ChartsPageServer.startIfNotStarted() }
            }

            // Load chart webpage
            webViewState.load(ChartsPageServer.getUrl())

            // Await page load
            webViewState.loadState.first { loadState -> loadState == WebViewState.LoadState.LOADED }

            // Forward callbacks
            webViewState.createJSCallback("chartCallback")
                .messages
                .onEach { message ->
                    charts.forEach { chart -> chart.onCallback(message) }
                }
                .launchIn(coroutineScope)

            // Execute chart scripts
            merge(arrangement.scripts, scripts.consumeAsFlow()).onEach(webViewState::executeScript).collect()
        }
    }

    fun setPageBackgroundColor(color: Color) {
        scripts.trySend("setPageBackgroundColor('${color.toHexString()}');")
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
