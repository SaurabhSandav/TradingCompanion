package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.PriceScaleOptions
import com.saurabhsandav.core.chart.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.HistogramStyleOptions
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.ui.stockchart.StockChart

class VolumePlotter(
    override val key: String,
    override val legendLabel: String = "Vol",
) : SeriesPlotter<HistogramData>() {

    override fun legendText(params: MouseEventParams): String {
        val volume = series.let { (params.seriesData[it] as? SingleValueData?)?.value?.toString() }.orEmpty()
        return "$legendLabel $volume"
    }

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
}
