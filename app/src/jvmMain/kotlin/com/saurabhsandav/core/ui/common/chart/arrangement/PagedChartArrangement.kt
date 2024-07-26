package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.callbacks.ChartCallback
import com.saurabhsandav.lightweight_charts.options.ChartOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

fun ChartArrangement.Companion.paged(): PagedChartArrangement {
    return PagedChartArrangement()
}

class PagedChartArrangement internal constructor() : ChartArrangement() {

    private val charts = mutableListOf<IChartApi>()
    private val _lastActiveChart = MutableSharedFlow<IChartApi>(replay = 1)

    val lastActiveChart = _lastActiveChart.asSharedFlow()

    override fun onCallback(message: String): Boolean {

        val callbackElement = Json.parseToJsonElement(message)

        val chartCallback = ChartCallback(
            chartName = callbackElement.jsonObject["chartName"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        val chart = charts.find { it.name == chartCallback.chartName }

        return when {
            chartCallback.callbackType == "ChartInteraction" && chart != null -> {

                val messageElement = Json.parseToJsonElement(chartCallback.message)

                when (messageElement.jsonPrimitive.content) {
                    "mouseenter" -> {
                        _lastActiveChart.tryEmit(chart)
                        true
                    }

                    else -> false
                }
            }

            else -> false
        }
    }

    fun newChart(
        options: ChartOptions = ChartOptions(),
    ): IChartApi {

        val chartId = "chart_${Random.nextLong()}"

        // Error if chart name already exists
        check(!charts.any { it.name == chartId })

        // Configure hidden chart container
        executeJs("preparePagedChartContainer('$chartId');")

        val chart = com.saurabhsandav.lightweight_charts.createChart(
            container = "document.getElementById('$chartId')",
            options = options.copy(autoSize = true),
            name = chartId,
        )

        // Add to tabs
        charts.add(chart)

        return chart
    }

    fun removeChart(chart: IChartApi) {

        // Delete chart div
        executeJs("document.getElementById('${chart.name}').remove();")

        // Remove tab
        charts.remove(chart)
    }

    fun showChart(chart: IChartApi) {

        // Hide all chart divs, then show selected chart div
        executeJs("showPagedChart('${chart.name}');")
    }

    fun setLegend(
        chart: IChartApi,
        legendHtmlItems: List<String>,
    ) {
        executeJs("setPagedLegendTexts('${chart.name}', [${legendHtmlItems.joinToString(", ") { "'$it'" }}]);")
    }
}
