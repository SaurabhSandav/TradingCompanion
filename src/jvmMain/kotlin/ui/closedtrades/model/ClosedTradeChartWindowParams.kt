package ui.closedtrades.model

import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.SeriesMarker

internal class ClosedTradeChartWindowParams(
    val tradeId: Int,
    val chartData: ClosedTradeChartData,
)

data class ClosedTradeChartData(
    val candleData: List<CandlestickData>,
    val volumeData: List<HistogramData>,
    val ema9Data: List<LineData>,
    val vwapData: List<LineData>,
    val visibilityIndexRange: ClosedRange<Int>,
    val markers: List<SeriesMarker>,
)
