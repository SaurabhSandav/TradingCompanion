package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

class ReplaySeries(
    replaySeries: CandleSeries,
    val replayTime: StateFlow<Instant>,
    val candleState: StateFlow<BarReplay.CandleState>,
) : CandleSeries by replaySeries

internal interface ReplaySeriesBuilder {

    val replaySeries: ReplaySeries

    fun addCandle(offset: Int)

    fun addCandle(offset: Int, candleState: BarReplay.CandleState)

    fun reset()
}

enum class CandleUpdateType {
    FullBar,
    OHLC;
}
