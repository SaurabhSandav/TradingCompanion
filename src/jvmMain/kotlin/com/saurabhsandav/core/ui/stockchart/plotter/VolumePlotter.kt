package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.PriceScaleOptions
import com.saurabhsandav.core.chart.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.options.HistogramStyleOptions
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VolumePlotter(
    override val key: String,
    override val legendLabel: String = "Vol",
) : SeriesPlotter<HistogramData>() {

    override fun createSeries(chart: StockChart): ISeriesApi<HistogramData> {

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
                    bottom = 0,
                )
            )
        )

        return series
    }

    override fun legendText(chart: StockChart): Flow<String> = chart.actualChart.crosshairMove().map { params ->
        val volume = (params.seriesData[series] as? SingleValueData?)?.value?.toString().orEmpty()
        "$legendLabel $volume"
    }
}
