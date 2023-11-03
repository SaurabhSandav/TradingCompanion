package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.CandlestickStyleOptions
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.ui.stockchart.StockChart

class CandlestickPlotter(
    override val key: String,
) : SeriesPlotter<CandlestickData>() {

    override var legendLabel: String = ""

    override fun legendText(params: MouseEventParams): String {
        val candlestickSeriesPrices = params.seriesData[series] as? CandlestickData?
        val open = candlestickSeriesPrices?.open?.toString().orEmpty()
        val high = candlestickSeriesPrices?.high?.toString().orEmpty()
        val low = candlestickSeriesPrices?.low?.toString().orEmpty()
        val close = candlestickSeriesPrices?.close?.toString().orEmpty()
        return "$legendLabel O $open H $high L $low C $close"
    }

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
}
