package com.saurabhsandav.core.ui.common.chart.arrangement

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.createChart
import com.saurabhsandav.core.chart.options.ChartOptions

fun ChartArrangement.Companion.paged(): PagedChartArrangement {
    return PagedChartArrangement()
}

class PagedChartArrangement internal constructor() : ChartArrangement() {

    private val charts = mutableSetOf<IChartApi>()

    fun newChart(
        name: String,
        options: ChartOptions = ChartOptions(),
    ): IChartApi {

        // Error if chart name already exists
        check(!charts.any { it.name == name })

        // Create hidden div for new chart
        executeJs(
            """|
            |(function() {
            |  var iDiv = document.createElement('div');
            |  iDiv.id = '$name';
            |  iDiv.className = 'tabcontent';
            |  iDiv.style.display = "none";
            |  document.body.appendChild(iDiv);
            |})()
            """.trimMargin()
        )

        val chart = createChart(
            container = "document.getElementById('$name')",
            options = options,
            name = name,
        )

        // Add to tabs
        charts += chart

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
        executeJs(
            """|
            |(function() {
            |  var tabcontent = document.getElementsByClassName("tabcontent");
            |  for (i = 0; i < tabcontent.length; i++) {
            |    tabcontent[i].style.display = "none";
            |  }
            |  
            |  document.getElementById('${chart.name}').style.display = "block";
            |})()
            """.trimMargin()
        )
    }
}
