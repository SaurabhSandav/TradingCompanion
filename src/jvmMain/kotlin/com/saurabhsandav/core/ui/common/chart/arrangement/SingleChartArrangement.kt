package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.callbacks.ChartCallback
import com.saurabhsandav.core.chart.createChart
import com.saurabhsandav.core.chart.options.ChartOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun ChartArrangement.Companion.single(): SingleChartArrangement {
    return SingleChartArrangement()
}

class SingleChartArrangement internal constructor() : ChartArrangement() {

    private val chartId = "chart"
    private var chart: IChartApi? = null

    override fun onCallback(message: String): Boolean {

        val callbackElement = Json.parseToJsonElement(message)

        val chartCallback = ChartCallback(
            chartName = callbackElement.jsonObject["chartName"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        return chartCallback.callbackType == "ChartInteraction" && chart != null
    }

    fun newChart(
        options: ChartOptions = ChartOptions(),
    ): IChartApi {

        val chart = chart

        if (chart == null) {
            // Configure chart container
            executeJs("prepareSingleChartContainer('$chartId');")
        } else {
            // Remove chart if exists
            chart.remove()
        }

        // Create new chart
        return createChart(
            container = "document.getElementById('$chartId')",
            options = options,
            name = chartId,
        ).also { this.chart = it }
    }

    fun setLegend(legendHtmlItems: List<String>) {
        executeJs("setSingleLegendTexts([${legendHtmlItems.joinToString(", ") { "'$it'" }}]);")
    }
}
