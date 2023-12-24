package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class SimpleReplaySeriesBuilder(
    private val inputSeries: CandleSeries,
    private val initialIndex: Int,
) : ReplaySeriesBuilder {

    private val _replaySeries = MutableCandleSeries(
        initial = inputSeries.subList(0, initialIndex),
        timeframe = inputSeries.timeframe,
    )
    private val _replayTime = MutableStateFlow(_replaySeries.last().openInstant)
    private val _candleState = MutableStateFlow(BarReplay.CandleState.Close)

    override val replaySeries: ReplaySeries = ReplaySeries(
        replaySeries = _replaySeries,
        replayTime = _replayTime.asStateFlow(),
        candleState = _candleState.asStateFlow(),
    )

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

        // Update candle state
        _candleState.update { candleState }
    }

    override fun reset() {

        // Reset replaySeries to initial state
        _replaySeries.removeLast(_replaySeries.size - initialIndex)

        // Update time
        _replayTime.update { _replaySeries.last().openInstant }

        // Update candle state
        _candleState.update { BarReplay.CandleState.Close }
    }
}
