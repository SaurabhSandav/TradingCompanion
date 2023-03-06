package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.options.CandlestickStyleOptions

class CandlestickPlotter(
    private val chart: IChartApi,
    mapper: (index: Int) -> CandlestickData,
) : SeriesPlotter<CandlestickData>(chart, mapper) {

    override fun legendText(params: MouseEventParams): String {
        val series = series ?: return ""
        val candlestickSeriesPrices = params.seriesData[series] as? CandlestickData?
        val open = candlestickSeriesPrices?.open?.toString().orEmpty()
        val high = candlestickSeriesPrices?.high?.toString().orEmpty()
        val low = candlestickSeriesPrices?.low?.toString().orEmpty()
        val close = candlestickSeriesPrices?.close?.toString().orEmpty()
        return "O $open H $high L $low C $close"
    }

    override fun createSeries(): ISeriesApi<CandlestickData> {
        return chart.addCandlestickSeries(
            name = "candlestickSeries",
            options = CandlestickStyleOptions(
                lastValueVisible = false,
            ),
        )
    }
}
