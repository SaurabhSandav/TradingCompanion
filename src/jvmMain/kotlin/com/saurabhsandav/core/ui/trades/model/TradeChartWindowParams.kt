package com.saurabhsandav.core.ui.trades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal class TradeChartWindowParams(
    val profileTradeId: ProfileTradeId,
    val chartData: TradeChartData,
)

@Immutable
data class TradeChartData(
    val candleData: ImmutableList<CandlestickData>,
    val volumeData: ImmutableList<HistogramData>,
    val ema9Data: ImmutableList<LineData>,
    val vwapData: ImmutableList<LineData>,
    val visibilityIndexRange: ClosedRange<Int>,
    val markers: ImmutableList<SeriesMarker>,
)
