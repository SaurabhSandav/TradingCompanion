package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant

internal class ReplayCandleSource(
    override val params: StockChartParams,
    private val replaySeriesFactory: suspend () -> ReplaySeries,
    private val getTradeMarkers: (ClosedRange<Instant>) -> Flow<List<TradeMarker>>,
    private val getTradeExecutionMarkers: (ClosedRange<Instant>) -> Flow<List<TradeExecutionMarker>>,
) : CandleSource {

    val replaySeries = CompletableDeferred<ReplaySeries>()

    override suspend fun getCandleSeries(): CandleSeries = replaySeries.await()

    override suspend fun onLoad() {

        if (!replaySeries.isCompleted) {
            replaySeries.complete(replaySeriesFactory())
        }
    }

    override fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {
        return flow { emitAll(getTradeMarkers.invoke(instantRange)) }
    }

    override fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> {
        return flow { emitAll(getTradeExecutionMarkers.invoke(instantRange)) }
    }
}
