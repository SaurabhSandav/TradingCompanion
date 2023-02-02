package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.subList
import com.saurabhsandav.core.subListInclusive
import com.saurabhsandav.core.trading.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

class ResampledBarReplaySession(
    override val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    currentCandleState: BarReplay.CandleState,
    private val timeframeSeries: CandleSeries,
    private val isSessionStart: (CandleSeries, Int) -> Boolean,
) : BarReplaySession {

    private val _replaySeries: MutableCandleSeries
    private var currentTimeframeCandleIndex: Int
    private val _replayTime: MutableStateFlow<Instant>

    init {

        val currentIndex = initialIndex - 1 + currentOffset

        // Find timeframe candle index that corresponds to current replay time
        currentTimeframeCandleIndex = findTimeframeCandleIndex(inputSeries[currentIndex])

        // Last candle is resampled from inputSeries to make it more accurate to current replay state
        val resampledCandle = resampleCandleAt(offset = currentOffset)

        // Init replay series
        _replaySeries = MutableCandleSeries(
            initial = timeframeSeries.subList(0, toIndexExclusive = currentTimeframeCandleIndex) + resampledCandle,
            timeframe = timeframeSeries.timeframe,
        )

        // If candle is not closed, a new partially formed candle needs to be added.
        if (currentCandleState != BarReplay.CandleState.Close)
            addCandle(currentOffset, currentCandleState)

        // Set initial time
        _replayTime = MutableStateFlow(_replaySeries.last().openInstant)
    }

    override val replaySeries: CandleSeries = _replaySeries.asCandleSeries()

    override val replayTime: StateFlow<Instant> = _replayTime.asStateFlow()

    override fun addCandle(offset: Int) {

        val currentIndex = initialIndex + offset

        val candle = when {
            isResampleCandleStart(inputSeries, currentIndex, timeframeSeries.timeframe) -> {

                // Replace previous finished candle with timeframe candle
                _replaySeries.removeLast()
                _replaySeries.addCandle(timeframeSeries[currentTimeframeCandleIndex])

                // Increment current timeframe candle
                currentTimeframeCandleIndex++

                // Candle as-is with openInstant adjusted
                inputSeries[currentIndex].withTimeframeOpenInstant()
            }

            // New candle from inputSeries is resampled into previously added candle
            else -> _replaySeries.last().resample(inputSeries[currentIndex]).withTimeframeOpenInstant()
        }

        // Add to replay series
        _replaySeries.addCandle(candle)

        // Update time
        _replayTime.update { inputSeries[currentIndex].openInstant }
    }

    override fun addCandle(offset: Int, candleState: BarReplay.CandleState) {

        val currentIndex = initialIndex + offset

        val candle = when {
            isResampleCandleStart(inputSeries, currentIndex, timeframeSeries.timeframe)
                    && candleState == BarReplay.CandleState.Open -> {

                // Replace previous finished candle with timeframe candle
                _replaySeries.removeLast()
                _replaySeries.addCandle(timeframeSeries[currentTimeframeCandleIndex])

                // Increment current timeframe candle
                currentTimeframeCandleIndex++

                // Candle as-is with openInstant adjusted
                inputSeries[currentIndex].atState(BarReplay.CandleState.Open).withTimeframeOpenInstant()
            }

            // New candle from inputSeries is resampled into previously added candle
            else -> _replaySeries.last().resample(inputSeries[currentIndex].atState(candleState))
                .withTimeframeOpenInstant()
        }

        // Add to replay series
        _replaySeries.addCandle(candle)

        // Update time
        _replayTime.update { inputSeries[currentIndex].openInstant }
    }

    override fun reset() {

        currentTimeframeCandleIndex = findTimeframeCandleIndex(inputSeries[initialIndex - 1])

        // Get timeframe candle before last
        val timeframeCandle = timeframeSeries[currentTimeframeCandleIndex - 1]

        // Remove all candles not included in initial interval
        while (_replaySeries.lastOrNull() != timeframeCandle) {
            _replaySeries.removeLast()
        }

        // Last candle is resampled from inputSeries to make it more accurate to current replay state
        val resampledCandle = resampleCandleAt(offset = 0)

        // Add resampled candle
        _replaySeries.addCandle(resampledCandle)

        // Update time
        _replayTime.update { resampledCandle.openInstant }
    }

    /**
     * Find index of the corresponding candle in timeframeSeries that contains the given candle.
     */
    private fun findTimeframeCandleIndex(candle: Candle): Int {
        // If candle.openInstant is less than the current timeframeCandle.openInstant, we want the previous candle.
        return timeframeSeries.indexOfFirst { timeframeCandle -> candle.openInstant < timeframeCandle.openInstant } - 1
    }

    /**
     * Replace openInstant with the current Timeframe candle openInstant
     *
     * The 1D timeframe openInstant in Indian markets is not the openInstant of the first candle in lower timeframes.
     * Workaround such cases by using timeframe openInstant.
     */
    private fun Candle.withTimeframeOpenInstant(): Candle {
        return copy(openInstant = timeframeSeries[currentTimeframeCandleIndex].openInstant)
    }

    private fun resampleCandleAt(offset: Int): Candle {

        val currentIndex = initialIndex - 1 + offset
        val timeframeCandle = timeframeSeries[currentTimeframeCandleIndex]

        // From a list of already replayed candles, find index of candle which marks the start of the current candle
        // in the given timeframe.
        val currentResampleCandleStartIndex = inputSeries.subListInclusive(0, currentIndex)
            .indexOfLast { it.openInstant < timeframeCandle.openInstant } + 1

        // Resample
        return inputSeries.subListInclusive(currentResampleCandleStartIndex, currentIndex)
            .reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
            .withTimeframeOpenInstant()
    }

    private fun isResampleCandleStart(
        candleSeries: CandleSeries,
        index: Int,
        timeframe: Timeframe,
    ): Boolean {

        // Session start is also candle start
        if (isSessionStart(candleSeries, index)) return true

        // Find candle for current session start
        // If no session start found, default to the first candle
        var sessionStartCandle = candleSeries.first()

        for (i in candleSeries.indices.reversed()) {
            if (isSessionStart(candleSeries, i)) {
                sessionStartCandle = candleSeries[i]
                break
            }
        }

        // If interval from session start to current candle is multiple of timeframe,
        // candle is start of resample candle.
        val currentCandleEpochSeconds = candleSeries[index].openInstant.epochSeconds
        val secondsSinceSessionStart = sessionStartCandle.openInstant.epochSeconds - currentCandleEpochSeconds
        if (secondsSinceSessionStart.rem(timeframe.seconds) == 0L) return true

        // Not candle start
        return false
    }

    private fun Candle.resample(newCandle: Candle): Candle = copy(
        high = if (high > newCandle.high) high else newCandle.high,
        low = if (low < newCandle.low) low else newCandle.low,
        close = newCandle.close,
        volume = volume + newCandle.volume,
    )
}
