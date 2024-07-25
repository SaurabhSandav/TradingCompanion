package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.options.LineStyleOptions
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LinePlotter(
    override val key: String,
    override val legendLabel: String,
    private val color: Color? = null,
) : SeriesPlotter<LineData>() {

    override fun createSeries(chart: StockChart): ISeriesApi<LineData> {

        var options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        )

        if (color != null) options = options.copy(color = color)

        return chart.actualChart.addLineSeries(
            name = key,
            options = options,
        )
    }

    override fun legendText(chart: StockChart): Flow<String> = chart.actualChart.crosshairMove().map { params ->
        val value = (params.seriesData[series] as? SingleValueData?)?.value?.toString().orEmpty()
        "$legendLabel $value"
    }
}
