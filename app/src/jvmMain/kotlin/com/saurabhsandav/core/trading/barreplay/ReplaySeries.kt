package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.trading.core.CandleSeries
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

class ReplaySeries(
    replaySeries: CandleSeries,
    val replayTime: StateFlow<Instant>,
    val candleState: StateFlow<BarReplay.CandleState>,
) : CandleSeries by replaySeries

internal interface ReplaySeriesBuilder {

    val replaySeries: ReplaySeries

    fun getNextCandleInstant(): Instant?

    fun advanceTo(
        instant: Instant,
        candleState: BarReplay.CandleState,
    )

    fun reset()
}

enum class CandleUpdateType {
    FullBar,
    OHLC,
}
