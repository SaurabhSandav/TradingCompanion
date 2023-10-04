package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.PriceScaleOptions
import com.saurabhsandav.core.chart.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.SingleValueData
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.HistogramStyleOptions
import com.saurabhsandav.core.chart.options.common.PriceFormat

class VolumePlotter(
    private val chart: IChartApi,
    override val name: String = "Vol",
) : SeriesPlotter<HistogramData>(chart) {

    override fun legendText(params: MouseEventParams): String {
        val volume = series?.let { (params.seriesData[it] as? SingleValueData?)?.value?.toString() }.orEmpty()
        return "$name $volume"
    }

    override fun createSeries(): ISeriesApi<HistogramData> {

        val series = chart.addHistogramSeries(
            name = "volumeSeries",
            options = HistogramStyleOptions(
                lastValueVisible = false,
                priceFormat = PriceFormat.BuiltIn(
                    type = PriceFormat.Type.Volume,
                ),
                priceScaleId = "",
                priceLineVisible = false,
            ),
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
