package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant

interface BarReplaySession {

    val inputSeries: CandleSeries

    val replaySeries: CandleSeries

    val replayTime: StateFlow<Instant>

    fun addCandle(offset: Int)

    fun addCandle(offset: Int, candleState: BarReplay.CandleState)

    fun reset()
}

enum class CandleUpdateType {
    FullBar,
    OHLC;
}
