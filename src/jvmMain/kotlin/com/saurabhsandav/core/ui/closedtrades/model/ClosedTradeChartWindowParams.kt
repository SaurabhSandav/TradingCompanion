package com.saurabhsandav.core.ui.closedtrades.model

import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.options.PriceLineOptions

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
