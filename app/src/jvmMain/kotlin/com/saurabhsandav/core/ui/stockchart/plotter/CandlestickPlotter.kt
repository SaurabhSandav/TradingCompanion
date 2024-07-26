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
        val series = series
        val candlestickSeriesPrices = params.seriesData[series] as? CandlestickData?
        val open = candlestickSeriesPrices?.open?.toString().orEmpty()
        val high = candlestickSeriesPrices?.high?.toString().orEmpty()
        val low = candlestickSeriesPrices?.low?.toString().orEmpty()
        val close = candlestickSeriesPrices?.close?.toString().orEmpty()
        "$legendLabel O $open H $high L $low C $close"
    }
}
