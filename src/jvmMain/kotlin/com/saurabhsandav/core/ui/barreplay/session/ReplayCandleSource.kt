package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant

internal class ReplayCandleSource(
    override val params: StockChartParams,
    private val replaySeriesFactory: suspend () -> ReplaySeries,
    private val getTradeMarkers: (ClosedRange<Instant>) -> Flow<List<TradeMarker>>,
    private val getTradeExecutionMarkers: (ClosedRange<Instant>) -> Flow<List<TradeExecutionMarker>>,
) : CandleSource {

    val replaySeries = CompletableDeferred<ReplaySeries>()

    override suspend fun onLoad(interval: ClosedRange<Instant>): CandleSource.Result {

        val replaySeries = loadReplaySeries()

        val fromIndex = replaySeries
            .binarySearchByAsResult(interval.start) { it.openInstant }
            .indexOrNaturalIndex
        val lastIndex = replaySeries
            .binarySearchByAsResult(interval.endInclusive) { it.openInstant }
            .indexOr { naturalIndex -> naturalIndex - 1 }
            .coerceAtMost(replaySeries.lastIndex)
        val toIndex = lastIndex + 1

        return CandleSource.Result(
            candles = replaySeries.subList(fromIndex, toIndex),
            live = replaySeries.live.takeIf { toIndex == replaySeries.size },
        )
    }

    override suspend fun onLoadBefore(
        before: Instant,
        count: Int,
    ): CandleSource.Result {

        val replaySeries = loadReplaySeries()

        val lastIndex = replaySeries
            .binarySearchByAsResult(before) { it.openInstant }
            .indexOr { naturalIndex -> naturalIndex - 1 }
            .coerceAtMost(replaySeries.lastIndex)
        val fromIndex = (lastIndex - count).coerceAtLeast(0)
        val toIndex = lastIndex + 1

        return CandleSource.Result(
            candles = replaySeries.subList(fromIndex, toIndex),
            live = replaySeries.live.takeIf { toIndex == replaySeries.size },
        )
    }

    override suspend fun onLoadAfter(
        after: Instant,
        count: Int,
    ): CandleSource.Result {

        val replaySeries = loadReplaySeries()

        val fromIndex = replaySeries.binarySearchByAsResult(after) { it.openInstant }.indexOrNaturalIndex
        val toIndex = (fromIndex + count + 1).coerceAtMost(replaySeries.size)

        return CandleSource.Result(
            candles = replaySeries.subList(fromIndex, toIndex),
            live = replaySeries.live.takeIf { toIndex == replaySeries.size },
        )
    }

    private suspend fun loadReplaySeries(): ReplaySeries {

        if (!replaySeries.isCompleted) {
            replaySeries.complete(replaySeriesFactory())
        }

        return replaySeries.await()
    }

    override fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {
        return flow { getTradeMarkers.invoke(instantRange).emitInto(this) }
    }

    override fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> {
        return flow { getTradeExecutionMarkers.invoke(instantRange).emitInto(this) }
    }
}
