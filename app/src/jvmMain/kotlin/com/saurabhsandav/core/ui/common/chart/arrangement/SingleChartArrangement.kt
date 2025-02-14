package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.options.ChartOptions

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
        return com.saurabhsandav.lightweight_charts.createChart(
            container = "document.getElementById('$chartId')",
            options = options.copy(autoSize = true),
            id = chartId,
        ).also { this.chart = it }
    }
}
