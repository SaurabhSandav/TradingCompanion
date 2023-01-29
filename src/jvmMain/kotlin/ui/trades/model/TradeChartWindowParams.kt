package ui.trades.model

import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.SeriesMarker

internal class TradeChartWindowParams(
    val tradeId: Long,
    val chartData: TradeChartData,
)

data class TradeChartData(
    val candleData: List<CandlestickData>,
    val volumeData: List<HistogramData>,
    val ema9Data: List<LineData>,
    val vwapData: List<LineData>,
    val visibilityIndexRange: ClosedRange<Int>,
    val markers: List<SeriesMarker>,
)
