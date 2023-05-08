package com.saurabhsandav.core.ui.barreplay.charts

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ReplayCandleSource(
    override val ticker: String,
    override val timeframe: Timeframe,
    private val replaySeriesFactory: suspend () -> ReplaySeries,
    getMarkers: (CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    val replaySeries = CompletableDeferred<ReplaySeries>()

    override val hasVolume: Boolean = ticker != "NIFTY50"

    private var _candleSeries: CandleSeries? = null
    override val candleSeries: CandleSeries
        get() = checkNotNull(_candleSeries) { "CandleSeries not loaded" }

    override val syncKey = timeframe

    override val candleMarkers: Flow<List<SeriesMarker>> = flow {
        val candleSeries = replaySeries.await()
        emitAll(getMarkers(candleSeries))
    }

    override suspend fun onLoad() {

        if (!replaySeries.isCompleted) {
            replaySeries.complete(replaySeriesFactory())
        }

        _candleSeries = replaySeries.await()
    }
}
