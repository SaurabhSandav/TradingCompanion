package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.Timeframe
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import kotlin.time.Instant

class BarReplay(
    private val timeframe: Timeframe,
    private val from: Instant,
    private val candleUpdateType: CandleUpdateType = CandleUpdateType.FullBar,
) {

    private var currentInstant = from
    private var candleState = CandleState.Close

    private val replaySeriesBuilders = mutableListOf<ReplaySeriesBuilder>()

    fun newSeries(
        inputSeries: CandleSeries,
        timeframeSeries: CandleSeries? = null,
    ): ReplaySeries {

        check(timeframe == inputSeries.timeframe) { "BarReplay: Input series timeframe is invalid" }

        val initialIndex = inputSeries
            .binarySearchByAsResult(from) { it.openInstant }
            .indexOrNaturalIndex

        val replaySeriesBuilder = when (timeframeSeries) {
            null -> SimpleReplaySeriesBuilder(
                inputSeries = inputSeries,
                initialIndex = initialIndex,
            )

            else -> ResampledReplaySeriesBuilder(
                inputSeries = inputSeries,
                initialIndex = initialIndex,
                timeframeSeries = timeframeSeries,
            )
        }

        // Advance to current instant and candle state
        replaySeriesBuilder.advanceTo(currentInstant, candleState)

        replaySeriesBuilders.add(replaySeriesBuilder)

        return replaySeriesBuilder.replaySeries
    }

    fun removeSeries(series: ReplaySeries) {
        replaySeriesBuilders.find { builder -> builder.replaySeries == series }?.let(replaySeriesBuilders::remove)
    }

    fun advance(): Boolean {

        val nextInstant = when (candleState) {
            CandleState.Close ->
                replaySeriesBuilders
                    .mapNotNull { builder -> builder.getNextCandleInstant() }
                    .minOrNull() ?: return false

            else -> currentInstant
        }

        candleState = when (candleUpdateType) {
            CandleUpdateType.FullBar -> CandleState.Close
            CandleUpdateType.OHLC -> candleState.next()
        }

        // Add/Update bar
        replaySeriesBuilders.forEach { builder -> builder.advanceTo(nextInstant, candleState) }

        currentInstant = nextInstant

        return true
    }

    fun advanceToClose(): Boolean {

        // For FullBar update type, advance() is all we need
        if (candleUpdateType == CandleUpdateType.FullBar) return advance()

        // If current candle was closed, advance to next candle open before checking for candle close
        do {
            if (!advance()) return false
        } while (candleState != CandleState.Close)

        return true
    }

    fun reset() {
        replaySeriesBuilders.forEach { builder -> builder.reset() }
        currentInstant = from
        candleState = CandleState.Close
    }

    enum class CandleState {
        Open,
        Extreme1,
        Extreme2,
        Close,
        ;

        fun next(): CandleState = when (this) {
            Open -> Extreme1
            Extreme1 -> Extreme2
            Extreme2 -> Close
            Close -> Open
        }
    }
}
