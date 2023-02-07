package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.createChart
import com.saurabhsandav.core.chart.options.ChartOptions

fun ChartArrangement.Companion.single(): SingleChartArrangement {
    return SingleChartArrangement()
}

class SingleChartArrangement internal constructor() : ChartArrangement() {

    private val chartId = "chart"
    private var chart: IChartApi? = null

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
