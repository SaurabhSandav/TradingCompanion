package com.saurabhsandav.core.ui.barreplay.charts

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.BarReplaySession
import com.saurabhsandav.core.ui.stockchart.CandleSource
import kotlinx.coroutines.CompletableDeferred

class ReplayCandleSource(
    override val ticker: String,
    override val timeframe: Timeframe,
    private val replaySessionBuilder: suspend (String, Timeframe) -> BarReplaySession,
) : CandleSource {

    val replaySession = CompletableDeferred<BarReplaySession>()

    override val hasVolume: Boolean = ticker != "NIFTY50"

    private var _candleSeries: CandleSeries? = null
    override val candleSeries: CandleSeries
        get() = checkNotNull(_candleSeries) { "CandleSeries not loaded" }

    override val syncKey = timeframe

    override suspend fun onLoad() {

        if (!replaySession.isCompleted) {
            val newReplaySession = replaySessionBuilder(ticker, timeframe)
            replaySession.complete(newReplaySession)
        }

        _candleSeries = replaySession.await().replaySeries
    }
}
