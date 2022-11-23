package ui.closedtrades.model

import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.SeriesMarker
import chart.options.PriceLineOptions

internal class ClosedTradeChartWindowParams(
    val tradeId: Long,
    val chartData: ClosedTradeChartData,
)

data class ClosedTradeChartData(
    val candleData: List<CandlestickData>,
    val volumeData: List<HistogramData>,
    val ema9Data: List<LineData>,
    val vwapData: List<LineData>,
    val visibilityIndexRange: ClosedRange<Int>,
    val markers: List<SeriesMarker>,
    val priceLines: List<PriceLineOptions>,
)
