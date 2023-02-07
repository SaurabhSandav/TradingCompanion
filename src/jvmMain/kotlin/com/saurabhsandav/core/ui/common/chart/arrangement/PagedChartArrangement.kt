package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.createChart
import com.saurabhsandav.core.chart.options.ChartOptions
import kotlin.random.Random

fun ChartArrangement.Companion.paged(): PagedChartArrangement {
    return PagedChartArrangement()
}

class PagedChartArrangement internal constructor() : ChartArrangement() {

    private val charts = mutableMapOf<IChartApi, Int>()

    fun newChart(
        options: ChartOptions = ChartOptions(),
    ): IChartApi {

        val chartId = "chart_${Random.nextLong()}"

        // Error if chart name already exists
        check(!charts.keys.any { it.name == chartId })

        // Configure hidden chart container
        executeJs("preparePagedChartContainer('$chartId');")

        val chart = createChart(
            container = "document.getElementById('$chartId')",
            options = options,
            name = chartId,
        )

        // Add to tabs
        charts[chart] = 0

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
