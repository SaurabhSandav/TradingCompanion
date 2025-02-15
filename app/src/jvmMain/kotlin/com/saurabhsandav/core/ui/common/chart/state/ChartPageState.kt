package com.saurabhsandav.core.ui.common.chart.state

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.toHexString
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.createChart
import com.saurabhsandav.lightweight_charts.options.ChartOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ChartPageState(
    private val coroutineScope: CoroutineScope,
    val webViewState: WebViewState,
) {

    private val scripts = Channel<String>(Channel.UNLIMITED)
    private val charts = mutableMapOf<IChartApi, Job>()

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
                    charts.keys.forEach { chart -> chart.onCallback(message) }
                }
                .launchIn(coroutineScope)

            // Execute chart scripts
            scripts.consumeAsFlow().onEach(webViewState::executeScript).collect()
        }
    }

    fun setPageBackgroundColor(color: Color) {
        executeJs("setPageBackgroundColor('${color.toHexString()}');")
    }

    fun addChart(
        options: ChartOptions = ChartOptions(),
        id: String = Uuid.random().toString(),
    ): IChartApi {

        // Error if chart id already exists
        check(!charts.keys.any { it.id == id })

        // Create hidden chart container
        executeJs("createChartContainer('$id');")

        val chart = createChart(
            container = "getChartContainer('$id')",
            options = options.copy(autoSize = true),
            id = id,
        )

        // Cache chart and send chart js scripts to WebView
        charts[chart] = coroutineScope.launch {
            chart.scripts.collect(::executeJs)
        }

        return chart
    }

    fun removeChart(chart: IChartApi) {

        // Delete chart container div
        executeJs("deleteChartContainer('${chart.id}');")

        // Remove chart from cache and stop collecting scripts
        charts.remove(chart)!!.cancel()
    }

    fun hideChart(chart: IChartApi) = executeJs("hideChart('${chart.id}');")

    fun showChart(chart: IChartApi) = executeJs("showChart('${chart.id}');")

    fun setChartLayout(
        chart: IChartApi,
        left: String,
        top: String,
        width: String,
        height: String,
    ) = executeJs("""setChartLayout("${chart.id}", "$left", "$top", "$width", "$height");""")

    private fun executeJs(script: String) {
        scripts.trySend(script)
    }
}
