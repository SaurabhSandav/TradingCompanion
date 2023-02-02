package com.saurabhsandav.core.ui.trades.model

import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SeriesMarker

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
