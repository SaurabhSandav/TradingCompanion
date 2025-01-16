package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.saurabhsandav.core.ui.common.toCssColor
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.data.LineData
import com.saurabhsandav.lightweight_charts.data.SeriesData
import com.saurabhsandav.lightweight_charts.options.LineStyleOptions
import com.saurabhsandav.lightweight_charts.options.common.LineWidth

class LinePlotter(
    override val key: String,
    override val legendLabel: AnnotatedString,
    private val color: Color? = null,
) : SeriesPlotter<LineData, LineStyleOptions>() {

    constructor(
        key: String,
        legendLabel: String,
        color: Color? = null,
    ) : this(key, AnnotatedString(legendLabel), color)

    private val legendValueStyle = SpanStyle(color = color ?: Color.Unspecified)

    override var legendText by mutableStateOf(AnnotatedString(""))
        private set

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

    override fun onUpdateLegendValues(seriesData: SeriesData?) {

        val value = (seriesData ?: latestValue)
            ?.let { it as? LineData.Item }
            ?.value
            ?.toString()
            .orEmpty()

        legendText = buildAnnotatedString {
            append(" ")
            withStyle(legendValueStyle) {
                append(value)
            }
        }
    }
}
