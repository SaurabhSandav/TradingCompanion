package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.utils.subList
import com.saurabhsandav.core.utils.subListInclusive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant

internal class ResampledReplaySeriesBuilder(
    private val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    private val currentCandleState: BarReplay.CandleState,
    private val timeframeSeries: CandleSeries,
) : ReplaySeriesBuilder {

    private val _replaySeries: MutableCandleSeries
    private var currentTimeframeCandleIndex: Int
    private val _replayTime: MutableStateFlow<Instant>
    private val _candleState = MutableStateFlow(currentCandleState)

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

        // Set initial time
        _replayTime = MutableStateFlow(_replaySeries.last().openInstant)

        // If candle is not closed, a new partially formed candle needs to be added.
        if (currentCandleState != BarReplay.CandleState.Close)
            addCandle(currentOffset, currentCandleState)
    }

    override val replaySeries: ReplaySeries = ReplaySeries(
        replaySeries = _replaySeries,
        replayTime = _replayTime.asStateFlow(),
        candleState = _candleState.asStateFlow(),
    )

    override fun addCandle(offset: Int) {

        val currentIndex = initialIndex + offset
        val inputCandle = inputSeries[currentIndex]

        val isNewTimeframeCandle =
            timeframeSeries.binarySearch { it.openInstant.compareTo(inputCandle.openInstant) } >= 0

        val candle = when {
            isNewTimeframeCandle -> {

                // Replace previous finished candle with timeframe candle
                _replaySeries.removeLast()
                _replaySeries.addLiveCandle(timeframeSeries[currentTimeframeCandleIndex])

                // Increment current timeframe candle
                currentTimeframeCandleIndex++

                // Candle as-is with openInstant adjusted
                inputCandle.withTimeframeOpenInstant()
            }

            // New candle from inputSeries is resampled into previously added candle
            else -> _replaySeries.last().resample(inputCandle).withTimeframeOpenInstant()
        }

        // Add to replay series
        _replaySeries.addLiveCandle(candle)

        // Update time
        _replayTime.update { inputCandle.openInstant }
    }

    override fun addCandle(offset: Int, candleState: BarReplay.CandleState) {

        val currentIndex = initialIndex + offset
        val inputCandle = inputSeries[currentIndex]

        val isNewTimeframeCandle =
            timeframeSeries.binarySearch { it.openInstant.compareTo(inputCandle.openInstant) } >= 0
                    && _replaySeries.last().openInstant != inputCandle.openInstant

        val candle = when {
            isNewTimeframeCandle -> {

                // Replace previous finished candle with timeframe candle
                _replaySeries.removeLast()
                _replaySeries.addLiveCandle(timeframeSeries[currentTimeframeCandleIndex])

                // Increment current timeframe candle
                currentTimeframeCandleIndex++

                // Candle as-is with openInstant adjusted
                inputCandle.atState(BarReplay.CandleState.Open).withTimeframeOpenInstant()
            }

            // New candle from inputSeries is resampled into previously added candle
            else -> _replaySeries.last().resample(inputCandle.atState(candleState))
                .withTimeframeOpenInstant()
        }

        // Add to replay series
        _replaySeries.addLiveCandle(candle)

        // Update time
        _replayTime.update { inputCandle.openInstant }

        // Update candle state
        _candleState.update { candleState }
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
        _replaySeries.addLiveCandle(resampledCandle)

        // Update time
        _replayTime.update { resampledCandle.openInstant }

        // Update candle state
        _candleState.update { currentCandleState }
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

    private fun Candle.resample(newCandle: Candle): Candle = copy(
        high = if (high > newCandle.high) high else newCandle.high,
        low = if (low < newCandle.low) low else newCandle.low,
        close = newCandle.close,
        volume = volume + newCandle.volume,
    )
}
