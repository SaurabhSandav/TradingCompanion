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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ChartPageState(
    private val coroutineScope: CoroutineScope,
    val webViewState: WebViewState,
) {

    private val scripts = Channel<String>(Channel.UNLIMITED)
    private val charts = mutableMapOf<String, ChartAndJob>()

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
                    charts.values.forEach { chartAndJob -> chartAndJob.chart.onCallback(message) }
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
        check(!charts.containsKey(id))

        // Create hidden chart container
        executeJs("createChartContainer('$id');")

        val chart = createChart(
            container = "getChartContainer('$id')",
            options = options.copy(autoSize = true),
            id = id,
        )

        // Cache chart and send chart js scripts to WebView
        charts[id] = ChartAndJob(
            chart = chart,
            job = coroutineScope.launch {
                chart.scripts.collect(::executeJs)
            },
        )

        return chart
    }

    fun removeChart(id: String) {

        // Delete chart container div
        executeJs("deleteChartContainer('$id');")

        // Remove chart from cache and stop collecting scripts
        charts.remove(id)!!.job.cancel()
    }

    fun hideChart(id: String) = executeJs("hideChart('$id');")

    fun showChart(id: String) = executeJs("showChart('$id');")

    fun setChartLayout(
        id: String,
        left: String,
        top: String,
        width: String,
        height: String,
    ) = executeJs("""setChartLayout("$id", "$left", "$top", "$width", "$height");""")

    private fun executeJs(script: String) {
        scripts.trySend(script)
    }

    class ChartAndJob(
        val chart: IChartApi,
        val job: Job,
    )
}
