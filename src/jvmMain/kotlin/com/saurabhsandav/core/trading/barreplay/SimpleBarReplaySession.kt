package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.utils.subList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

class SimpleBarReplaySession(
    override val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    currentCandleState: BarReplay.CandleState,
) : BarReplaySession {

    private val _replaySeries = MutableCandleSeries(
        initial = inputSeries.subList(0, toIndexExclusive = initialIndex + currentOffset),
        timeframe = inputSeries.timeframe,
    )
    private val _replayTime = MutableStateFlow(_replaySeries.last().openInstant)

    init {

        // If candle is not closed, a new partially formed candle needs to be added.
        if (currentCandleState != BarReplay.CandleState.Close)
            addCandle(currentOffset, currentCandleState)
    }

    override val replaySeries: CandleSeries = _replaySeries.asCandleSeries()

    override val replayTime: StateFlow<Instant> = _replayTime.asStateFlow()

    override fun addCandle(offset: Int) {

        // Get candle as-is
        val inputCandle = inputSeries[initialIndex + offset]

        // Add candle to replay series
        _replaySeries.addLiveCandle(inputCandle)

        // Update time
        _replayTime.update { inputCandle.openInstant }
    }

    override fun addCandle(offset: Int, candleState: BarReplay.CandleState) {

        // Get full closed candle
        val fullCandle = inputSeries[initialIndex + offset]

        // Simulate candle at given candle state
        val candle = fullCandle.atState(candleState)

        // Add candle to replay series
        _replaySeries.addLiveCandle(candle)

        // Update time
        _replayTime.update { candle.openInstant }
    }

    override fun reset() {

        // Reset replaySeries to initial state
        _replaySeries.removeLast(_replaySeries.size - initialIndex)

        // Update time
        _replayTime.update { _replaySeries.last().openInstant }
    }
}
