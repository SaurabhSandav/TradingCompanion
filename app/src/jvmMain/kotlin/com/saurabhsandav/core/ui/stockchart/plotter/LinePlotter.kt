package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.toCssColor
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.data.LineData
import com.saurabhsandav.lightweight_charts.options.LineStyleOptions
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LinePlotter(
    override val key: String,
    override val legendLabel: String,
    private val color: Color? = null,
) : SeriesPlotter<LineData, LineStyleOptions>() {

    override fun createSeries(chart: StockChart): ISeriesApi<LineData, LineStyleOptions> {

        var options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        )

        if (color != null) options = options.copy(color = color.toCssColor())

        return chart.actualChart.addLineSeries(
            name = key,
            options = options,
        )
    }

    override fun legendText(chart: StockChart): Flow<String> = chart.actualChart.crosshairMove().map { params ->

        val value = series.getMouseEventDataFrom(params.seriesData)
            ?.let { it as? LineData.Item }
            ?.value
            ?.toString()
            .orEmpty()

        "$legendLabel $value"
    }
}
