package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.LineStyleOptions
import com.saurabhsandav.core.chart.options.common.LineWidth

class LinePlotter(
    private val chart: IChartApi,
    private val name: String,
    private val color: Color? = null,
    override val isEnabled: () -> Boolean = { true },
    mapper: (index: Int) -> LineData,
) : SeriesPlotter<LineData>(chart, mapper) {

    override fun legendText(params: MouseEventParams): String {
        val value = series?.let { (params.seriesData[it] as? SingleValueData?)?.value?.toString() }.orEmpty()
        return "$name $value"
    }

    override fun createSeries(): ISeriesApi<LineData> {

        var options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        )

        if (color != null) options = options.copy(color = color)

        return chart.addLineSeries(
            name = name,
            options = options,
        )
    }
}
