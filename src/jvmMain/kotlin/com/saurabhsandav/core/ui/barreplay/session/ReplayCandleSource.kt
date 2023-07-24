package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class ReplayCandleSource(
    override val params: StockChartParams,
    private val replaySeriesFactory: suspend () -> ReplaySeries,
    private val getMarkers: (CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    val replaySeries = CompletableDeferred<ReplaySeries>()

    private var _candleSeries: CandleSeries? = null
    override val candleSeries: CandleSeries
        get() = checkNotNull(_candleSeries) { "CandleSeries not loaded" }

    override suspend fun onLoad() {

        if (!replaySeries.isCompleted) {
            replaySeries.complete(replaySeriesFactory())
        }

        _candleSeries = replaySeries.await()
    }

    override fun getCandleMarkers(): Flow<List<SeriesMarker>> {
        return flow { emitAll(getMarkers(replaySeries.await())) }
    }
}
