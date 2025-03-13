package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.saurabhsandav.core.ui.common.hex
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweightcharts.ISeriesApi
import com.saurabhsandav.lightweightcharts.PriceScaleOptions
import com.saurabhsandav.lightweightcharts.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.lightweightcharts.SeriesDefinition
import com.saurabhsandav.lightweightcharts.data.HistogramData
import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.options.HistogramStyleOptions
import com.saurabhsandav.lightweightcharts.options.common.PriceFormat

class VolumePlotter(
    override val key: String,
    override val legendLabel: AnnotatedString = AnnotatedString("Vol"),
) : SeriesPlotter<HistogramData, HistogramStyleOptions>() {

    override var legendText by mutableStateOf(AnnotatedString(""))
        private set

    override fun createSeries(chart: StockChart): ISeriesApi<HistogramData, HistogramStyleOptions> {

        val options = HistogramStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
            priceLineVisible = false,
        )

        val series = chart.actualChart.addSeries(
            definition = SeriesDefinition.HistogramSeries,
            id = key,
            options = options,
        )

        series.priceScale.applyOptions(
            PriceScaleOptions(
                scaleMargins = PriceScaleMargins(
                    top = 0.8,
                    bottom = 0.0,
                ),
            ),
        )

        return series
    }

    override fun onUpdateLegendValues(seriesData: SeriesData?) {

        val histogramData = (seriesData ?: latestValue) as? HistogramData.Item

        legendText = when (histogramData) {
            null -> AnnotatedString("")
            else -> {

                val color = histogramData.color?.let { Color.hex(it.value) } ?: Color.Unspecified
                val valueStyle = SpanStyle(color = color)

                buildAnnotatedString {
                    append(" ")
                    withStyle(valueStyle) {
                        append(histogramData.value.toString())
                    }
                }
            }
        }
    }
}
