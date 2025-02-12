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
            chartId = callbackElement.jsonObject["chartId"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        val chart = charts.find { it.id == chartCallback.chartId }

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
        id: String = "chart_${Random.nextLong()}",
    ): IChartApi {

        // Error if chart id already exists
        check(!charts.any { it.id == id })

        // Configure hidden chart container
        executeJs("preparePagedChartContainer('$id');")

        val chart = com.saurabhsandav.lightweight_charts.createChart(
            container = "document.getElementById('$id')",
            options = options.copy(autoSize = true),
            id = id,
        )

        // Add to tabs
        charts.add(chart)

        return chart
    }

    fun removeChart(chart: IChartApi) {

        // Delete chart div
        executeJs("document.getElementById('${chart.id}').remove();")

        // Remove tab
        charts.remove(chart)
    }

    fun showChart(chart: IChartApi) {

        // Hide all chart divs, then show selected chart div
        executeJs("showPagedChart('${chart.id}');")
    }
}
