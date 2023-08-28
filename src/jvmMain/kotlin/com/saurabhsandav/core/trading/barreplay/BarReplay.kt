package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.Timeframe

class BarReplay(
    private val timeframe: Timeframe,
    private val candleUpdateType: CandleUpdateType = CandleUpdateType.FullBar,
) {

    private var offset = 0
    private var candleState = CandleState.Close

    private val replaySeriesBuilders = mutableListOf<ReplaySeriesBuilder>()

    fun newSeries(
        inputSeries: CandleSeries,
        initialIndex: Int,
        timeframeSeries: CandleSeries? = null,
    ): ReplaySeries {

        check(timeframe == inputSeries.timeframe) { "BarReplay: Input series timeframe is invalid" }

        val replaySeriesBuilder = when (timeframeSeries) {
            null -> SimpleReplaySeriesBuilder(
                inputSeries = inputSeries,
                initialIndex = initialIndex,
                currentOffset = offset,
                currentCandleState = candleState,
            )

            else -> ResampledReplaySeriesBuilder(
                inputSeries = inputSeries,
                initialIndex = initialIndex,
                currentOffset = offset,
                currentCandleState = candleState,
                timeframeSeries = timeframeSeries,
                sessionChecker = DailySessionChecker,
            )
        }

        replaySeriesBuilders.add(replaySeriesBuilder)

        return replaySeriesBuilder.replaySeries
    }

    fun removeSeries(series: ReplaySeries) {
        replaySeriesBuilders.find { builder -> builder.replaySeries == series }?.let(replaySeriesBuilders::remove)
    }

    fun advance() {

        when (candleUpdateType) {
            CandleUpdateType.FullBar -> {
                replaySeriesBuilders.forEach { builder -> builder.addCandle(offset) }
                offset++
            }

            CandleUpdateType.OHLC -> {

                // Move to next candle state
                candleState = candleState.next()

                // Add/Update bar
                replaySeriesBuilders.forEach { builder -> builder.addCandle(offset, candleState) }

                // If candle has closed, increment offset
                if (candleState == CandleState.Close) offset++
            }
        }
    }

    fun advanceByBar() {

        // Move to candle close state
        candleState = CandleState.Close

        replaySeriesBuilders.forEach(
            when (candleUpdateType) {
                CandleUpdateType.FullBar -> { builder -> builder.addCandle(offset) }
                CandleUpdateType.OHLC -> { builder -> builder.addCandle(offset, candleState) }
            }
        )

        offset++
    }

    fun reset() {
        replaySeriesBuilders.forEach { builder -> builder.reset() }
        offset = 0
        candleState = CandleState.Close
    }

    enum class CandleState {
        Open,
        Extreme1,
        Extreme2,
        Close;

        fun next(): CandleState = when (this) {
            Open -> Extreme1
            Extreme1 -> Extreme2
            Extreme2 -> Close
            Close -> Open
        }
    }
}
