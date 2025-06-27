package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Instant

internal class ResampledReplaySeriesBuilder(
    private val inputSeries: CandleSeries,
    private val initialIndex: Int,
    private val timeframeSeries: CandleSeries,
) : ReplaySeriesBuilder {

    private val _replaySeries: MutableCandleSeries

    @Suppress("ktlint:standard:backing-property-naming")
    private val _replayTime: MutableStateFlow<Instant>

    @Suppress("ktlint:standard:backing-property-naming")
    private val _candleState = MutableStateFlow(BarReplay.CandleState.Close)

    private var currentInputIndex = initialIndex - 1
    private val initialTimeframeCandleIndex: Int

    init {

        // Find timeframe candle index that contains the current input candle
        initialTimeframeCandleIndex = findTimeframeCandleIndex(inputSeries[currentInputIndex])

        // Last candle is resampled to make it more accurate to current replay state
        val resampledLastCandle = timeframeCandleResampledTo(currentInputIndex)

        // Init replay series
        _replaySeries = MutableCandleSeries(
            initial = timeframeSeries.subList(0, initialTimeframeCandleIndex + 1) + resampledLastCandle,
            timeframe = timeframeSeries.timeframe,
        )

        // Set initial time
        _replayTime = MutableStateFlow(inputSeries[currentInputIndex].openInstant)
    }

    override val replaySeries: ReplaySeries = ReplaySeries(
        replaySeries = _replaySeries,
        replayTime = _replayTime.asStateFlow(),
        candleState = _candleState.asStateFlow(),
    )

    override fun getNextCandleInstant(): Instant? {
        return inputSeries.getOrNull(currentInputIndex + 1)?.openInstant
    }

    override fun advanceTo(
        instant: Instant,
        candleState: BarReplay.CandleState,
    ) {

        val advanceIndex = inputSeries.binarySearchByAsResult(
            key = instant,
            fromIndex = currentInputIndex,
            selector = { it.openInstant },
        ).indexOr { naturalIndex -> naturalIndex - 1 }

        (currentInputIndex..advanceIndex).forEach { index ->

            val inputCandle = inputSeries[index]
            val resampledNewCandle = timeframeCandleResampledTo(
                inputIndex = index,
                lastCandleState = if (index == advanceIndex) candleState else BarReplay.CandleState.Close,
            )

            // Add to replay series
            _replaySeries.addLiveCandle(resampledNewCandle)

            // Update time
            _replayTime.value = inputCandle.openInstant

            // Update candle state
            _candleState.value = candleState
        }

        currentInputIndex = advanceIndex
    }

    override fun reset() {

        // Reset offset
        currentInputIndex = initialIndex - 1

        // Last candle is resampled from inputSeries to make it more accurate to current replay state
        val resampledCandle = timeframeCandleResampledTo(currentInputIndex)

        // Reset ReplaySeries to initial state
        _replaySeries.replaceCandles(timeframeSeries.subList(0, initialTimeframeCandleIndex) + resampledCandle)

        // Update time
        _replayTime.value = inputSeries[currentInputIndex].openInstant

        // Update candle state
        _candleState.value = BarReplay.CandleState.Close
    }

    /**
     * Find index of the corresponding candle in timeframeSeries that contains the given candle.
     */
    private fun findTimeframeCandleIndex(candle: Candle): Int {
        // If candle.openInstant is less than the current timeframeCandle.openInstant, we want the previous candle.
        return timeframeSeries
            .binarySearchByAsResult(candle.openInstant) { it.openInstant }
            .indexOr { naturalIndex -> naturalIndex - 1 }
    }

    private fun timeframeCandleResampledTo(
        inputIndex: Int,
        lastCandleState: BarReplay.CandleState = BarReplay.CandleState.Close,
    ): Candle {

        val timeframeCandleIndex = findTimeframeCandleIndex(inputSeries[inputIndex])
        val timeframeCandle = timeframeSeries[timeframeCandleIndex]

        // From a list of already replayed candles, find index of candle which marks the start of the timeframe candle
        val currentResampleCandleStartIndex = inputSeries
            .binarySearchByAsResult(timeframeCandle.openInstant, toIndex = inputIndex + 1) { it.openInstant }
            .indexOrNaturalIndex

        val currentResampleCandleEndIndex = when {
            timeframeCandleIndex == timeframeSeries.lastIndex -> timeframeSeries.lastIndex
            else -> {

                val nextTimeframeCandle = timeframeSeries[timeframeCandleIndex + 1]

                // Find index of first input candle for the next timeframe candle
                // Subtract 1 from that index to get the last input candle in current timeframe candle
                inputSeries
                    .binarySearchByAsResult(
                        key = nextTimeframeCandle.openInstant,
                        fromIndex = currentInputIndex,
                        selector = { it.openInstant },
                    )
                    .indexOrNaturalIndex - 1
            }
        }

        // If input candle is closed, and it's the last candle in the timeframe candle, return original timeframe candle
        if (lastCandleState == BarReplay.CandleState.Close && inputIndex == currentResampleCandleEndIndex) {
            return timeframeCandle
        }

        // Resample into a new timeframe candle
        return inputSeries.subList(currentResampleCandleStartIndex, inputIndex + 1)
            // Handle partial last input candle
            .let { list ->
                if (lastCandleState == BarReplay.CandleState.Close) return@let list
                val resampledLast = list.last().atState(lastCandleState)
                list.dropLast(1) + resampledLast
            }
            .reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
            // The 1D timeframe openInstant may not the openInstant of the first candle in lower timeframes.
            // Workaround such cases by using timeframe openInstant.
            .copy(openInstant = timeframeCandle.openInstant)
    }

    private fun Candle.resample(newCandle: Candle): Candle = copy(
        high = if (high > newCandle.high) high else newCandle.high,
        low = if (low < newCandle.low) low else newCandle.low,
        close = newCandle.close,
        volume = volume + newCandle.volume,
    )
}
