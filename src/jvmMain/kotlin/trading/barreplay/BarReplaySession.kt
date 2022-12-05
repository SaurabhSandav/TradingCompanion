package trading.barreplay

import trading.*
import java.math.BigDecimal

interface BarReplaySession {

    val inputSeries: CandleSeries

    val replaySeries: CandleSeries

    fun addCandle(offset: Int)

    fun addCandle(offset: Int, candleState: BarReplay.CandleState)

    fun reset()

    fun resampled(timeframe: Timeframe): CandleSeries

    companion object {

        operator fun invoke(
            inputSeries: CandleSeries,
            initialIndex: Int,
            currentOffset: Int,
            currentCandleState: BarReplay.CandleState,
            isSessionStart: (CandleSeries, Int) -> Boolean,
        ): BarReplaySession = BarReplaySessionImpl(
            inputSeries = inputSeries,
            initialIndex = initialIndex,
            currentOffset = currentOffset,
            currentCandleState = currentCandleState,
            isSessionStart = isSessionStart,
        )
    }
}

enum class CandleUpdateType {
    FullBar,
    OHLC;
}

private class BarReplaySessionImpl(
    override val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    currentCandleState: BarReplay.CandleState,
    private val isSessionStart: (CandleSeries, Int) -> Boolean,
) : BarReplaySession {

    private val _replaySeries = MutableCandleSeries(
        initial = inputSeries.subList(0, initialIndex + currentOffset),
        timeframe = inputSeries.timeframe,
    )

    init {

        // If CandleState is not Close, a new partially formed candle is added to chart.
        if (currentCandleState != BarReplay.CandleState.Close) {
            val fullCandle = inputSeries[initialIndex + currentOffset]
            val candle = fullCandle.atState(currentCandleState)
            _replaySeries.addCandle(candle)
        }
    }

    private val resamplings = mutableMapOf<Timeframe, MutableCandleSeries>()

    override val replaySeries: CandleSeries = _replaySeries.asCandleSeries()

    override fun addCandle(offset: Int) {

        val index = initialIndex + offset
        val candle = inputSeries[index]

        _replaySeries.addCandle(candle)

        resamplings.forEach { (_, candleSeries) ->

            // Add candle unchanged if new candle start, else resample already added candle
            val resampledCandle = when {
                isResampleCandleStart(replaySeries, replaySeries.lastIndex, candleSeries.timeframe!!) -> candle
                else -> candleSeries.last().resample(candle)
            }

            // Add candle
            candleSeries.addCandle(resampledCandle)
        }
    }

    override fun addCandle(offset: Int, candleState: BarReplay.CandleState) {

        val index = initialIndex + offset
        val fullCandle = inputSeries[index]
        val candle = fullCandle.atState(candleState)

        _replaySeries.addCandle(candle)

        resamplings.forEach { (_, candleSeries) ->

            // Add candle unchanged if new candle start, else resample already added candle
            val resampledCandle = when {
                isResampleCandleStart(replaySeries, replaySeries.lastIndex, candleSeries.timeframe!!) -> candle
                else -> candleSeries.last().resample(candle)
            }

            // Add candle
            candleSeries.addCandle(resampledCandle)
        }
    }

    override fun reset() {

        // Reset replaySeries to initial state
        _replaySeries.removeLast(_replaySeries.size - initialIndex)

        // This is the simplest way of accomplishing this task. Replacing all candles in the resampledSeries would mean
        // notifying resampledSeries observers about every single candle.
        resamplings.values.forEach { resampledSeries ->

            // Resample candles for the given timeframe again
            val initialResampledCandles = initialResampleCandles(resampledSeries.timeframe!!)

            // Remove extra candles in resampledSeries based on the size of the initial resampled candles
            resampledSeries.removeLast(resampledSeries.size - initialResampledCandles.size)

            // Check if last candle in resampledSeries is same. If not, candle might've been resampled with more
            // up-to-date data and must be replaced to match replay series data.
            if (initialResampledCandles.last() != resampledSeries.last()) {
                resampledSeries.removeLast()
                resampledSeries.addCandle(initialResampledCandles.last())
            }
        }
    }

    override fun resampled(timeframe: Timeframe): CandleSeries {

        val inputTimeframe = requireNotNull(inputSeries.timeframe) { "inputSeries needs a timeframe" }

        // Can only resampled to greater timeframes
        check(timeframe.seconds > inputTimeframe.seconds) {
            "CandleSeries can only be resampled to greater timeframes"
        }

        // Check if resample possible
        check(timeframe.seconds.rem(inputTimeframe.seconds) == 0L) { "Cannot resample to $timeframe" }

        return resamplings.getOrPut(timeframe) {
            MutableCandleSeries(
                initial = initialResampleCandles(timeframe),
                timeframe = timeframe,
            )
        }.asCandleSeries()
    }

    private fun Candle.atState(state: BarReplay.CandleState): Candle {

        val isCandleBullish = close > open

        return when (state) {
            // Open
            BarReplay.CandleState.Open -> copy(
                high = open,
                low = open,
                close = open,
                volume = BigDecimal.ZERO
            )

            BarReplay.CandleState.Extreme1 -> when {
                // Bullish candle, update low first
                isCandleBullish -> copy(
                    high = open,
                    close = low,
                    volume = BigDecimal.ZERO
                )

                // Bearish candle, update high first
                else -> copy(
                    low = open,
                    close = high,
                    volume = BigDecimal.ZERO
                )
            }

            BarReplay.CandleState.Extreme2 -> when {
                // Bullish candle, update high second
                isCandleBullish -> copy(close = high, volume = BigDecimal.ZERO)
                // Bearish candle, update low second
                else -> copy(close = low, volume = BigDecimal.ZERO)
            }

            // Close
            BarReplay.CandleState.Close -> this
        }
    }

    private fun initialResampleCandles(timeframe: Timeframe): List<Candle> {
        return replaySeries.getResampleCandles(timeframe).map { candles ->
            candles.reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
        }
    }

    private fun CandleSeries.getResampleCandles(timeframe: Timeframe): List<List<Candle>> {

        // If no candles then no candles
        if (isEmpty()) return emptyList()

        val result = mutableListOf<List<Candle>>()

        var currentResampleCandleStartIndex = 0

        // Add a list of all candles for every resample candle to result
        indices.forEach { index ->

            // If it's resample candle start and index is 0, there is no candle before it to add
            if (isResampleCandleStart(this, index, timeframe) && index != 0) {

                // Add previous session
                result.add(subList(currentResampleCandleStartIndex, index))

                // Remember current resample candle start
                currentResampleCandleStartIndex = index
            }
        }

        // Add candles for last session
        result.add(subList(currentResampleCandleStartIndex, size))

        return result
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
