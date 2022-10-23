package ui.closedtrades.model

import chart.misc.SeriesMarker
import chart.series.candlestick.CandlestickData
import chart.series.histogram.HistogramData
import chart.series.line.LineData

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
