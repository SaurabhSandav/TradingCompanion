package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.PriceScaleOptions
import com.saurabhsandav.lightweight_charts.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.lightweight_charts.data.HistogramData
import com.saurabhsandav.lightweight_charts.options.HistogramStyleOptions
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VolumePlotter(
    override val key: String,
    override val legendLabel: String = "Vol",
) : SeriesPlotter<HistogramData, HistogramStyleOptions>() {

    override fun createSeries(chart: StockChart): ISeriesApi<HistogramData, HistogramStyleOptions> {

        val options = HistogramStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
            priceLineVisible = false,
        )

        val series = chart.actualChart.addHistogramSeries(
            name = key,
            options = options,
        )

        series.priceScale.applyOptions(
            PriceScaleOptions(
                scaleMargins = PriceScaleMargins(
                    top = 0.8,
                    bottom = 0.0,
                )
            )
        )

        return series
    }

    override fun legendText(chart: StockChart): Flow<String> = chart.actualChart.crosshairMove().map { params ->

        val volume = series.getMouseEventDataFrom(params.seriesData)
            ?.let { it as? HistogramData.Item }
            ?.value
            ?.toString()
            .orEmpty()

        "$legendLabel $volume"
    }
}
