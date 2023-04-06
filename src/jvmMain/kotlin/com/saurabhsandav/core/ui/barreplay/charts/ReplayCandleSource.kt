package com.saurabhsandav.core.ui.barreplay.charts

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.BarReplaySession
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReplayCandleSource(
    override val ticker: String,
    override val timeframe: Timeframe,
    private val replaySessionBuilder: suspend (String, Timeframe) -> BarReplaySession,
    getMarkers: (String, CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    val replaySession = CompletableDeferred<BarReplaySession>()

    override val hasVolume: Boolean = ticker != "NIFTY50"

    private var _candleSeries: CandleSeries? = null
    override val candleSeries: CandleSeries
        get() = checkNotNull(_candleSeries) { "CandleSeries not loaded" }

    override val syncKey = timeframe

    override val candleMarkers: Flow<List<SeriesMarker>> = flow {
        val candleSeries = replaySession.await().replaySeries
        emitAll(getMarkers(ticker, candleSeries))
    }

    override suspend fun onLoad() {

        if (!replaySession.isCompleted) {
            val newReplaySession = replaySessionBuilder(ticker, timeframe)
            replaySession.complete(newReplaySession)
        }

        _candleSeries = replaySession.await().replaySeries
    }
}
