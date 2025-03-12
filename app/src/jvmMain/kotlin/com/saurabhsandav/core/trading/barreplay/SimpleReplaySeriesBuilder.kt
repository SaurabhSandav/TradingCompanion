package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant

internal class SimpleReplaySeriesBuilder(
    private val inputSeries: CandleSeries,
    private val initialIndex: Int,
) : ReplaySeriesBuilder {

    private val _replaySeries = MutableCandleSeries(
        initial = inputSeries.subList(0, initialIndex),
        timeframe = inputSeries.timeframe,
    )

    @Suppress("ktlint:standard:backing-property-naming")
    private val _replayTime = MutableStateFlow(_replaySeries.last().openInstant)

    @Suppress("ktlint:standard:backing-property-naming")
    private val _candleState = MutableStateFlow(BarReplay.CandleState.Close)

    private var currentIndex = initialIndex - 1

    override val replaySeries: ReplaySeries = ReplaySeries(
        replaySeries = _replaySeries,
        replayTime = _replayTime.asStateFlow(),
        candleState = _candleState.asStateFlow(),
    )

    override fun getNextCandleInstant(): Instant? {
        return inputSeries.getOrNull(currentIndex + 1)?.openInstant
    }

    override fun advanceTo(
        instant: Instant,
        candleState: BarReplay.CandleState,
    ) {

        val advanceIndex = inputSeries.binarySearchByAsResult(
            key = instant,
            fromIndex = currentIndex,
            selector = { it.openInstant },
        ).indexOr { naturalIndex -> naturalIndex - 1 }

        (currentIndex..advanceIndex).forEach { index ->

            // Get full closed candle
            val fullCandle = inputSeries[index]

            // Simulate candle at given candle state
            val candle = when (index) {
                advanceIndex -> fullCandle.atState(candleState)
                else -> fullCandle
            }

            // Add candle to replay series
            _replaySeries.addLiveCandle(candle)

            // Update time
            _replayTime.value = candle.openInstant

            // Update candle state
            _candleState.value = candleState
        }

        currentIndex = advanceIndex
    }

    override fun reset() {

        // Reset offset
        currentIndex = initialIndex - 1

        // Reset replaySeries to initial state
        _replaySeries.replaceCandles(inputSeries.subList(0, initialIndex))

        // Update time
        _replayTime.value = _replaySeries.last().openInstant

        // Update candle state
        _candleState.value = BarReplay.CandleState.Close
    }
}
