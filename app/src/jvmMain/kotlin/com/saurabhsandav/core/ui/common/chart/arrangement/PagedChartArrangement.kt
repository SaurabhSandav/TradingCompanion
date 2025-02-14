package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.options.ChartOptions
import kotlin.random.Random

fun ChartArrangement.Companion.paged(): PagedChartArrangement {
    return PagedChartArrangement()
}

class PagedChartArrangement internal constructor() : ChartArrangement() {

    private val charts = mutableListOf<IChartApi>()

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
