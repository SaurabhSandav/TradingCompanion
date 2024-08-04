package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.lightweight_charts.ISeriesApi
import com.saurabhsandav.lightweight_charts.data.CandlestickData
import com.saurabhsandav.lightweight_charts.options.CandlestickStyleOptions
import com.saurabhsandav.lightweight_charts.options.common.PriceFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CandlestickPlotter(
    override val key: String,
) : SeriesPlotter<CandlestickData>() {

    override var legendLabel: String = ""

    override fun createSeries(chart: StockChart): ISeriesApi<CandlestickData> {

        val options = CandlestickStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Price,
                minMove = 0.05,
            ),
        )

        return chart.actualChart.addCandlestickSeries(
            name = key,
            options = options,
        )
    }

    override fun legendText(chart: StockChart): Flow<String> = chart.actualChart.crosshairMove().map { params ->

        val candlestickData = series
            .getMouseEventDataFrom(params.seriesData)
            ?.let { it as? CandlestickData.Item }

        buildString {
            append(legendLabel)
            append(" O ")
            candlestickData?.let { append(it.open.toString()) }
            append(" H ")
            candlestickData?.let { append(it.high.toString()) }
            append(" L ")
            candlestickData?.let { append(it.low.toString()) }
            append(" C ")
            candlestickData?.let { append(it.close.toString()) }
        }
    }
}
