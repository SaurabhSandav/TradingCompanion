package trading.barreplay

import trading.*

interface BarReplaySession {

    val inputSeries: CandleSeries

    val replaySeries: CandleSeries

    fun addCandle(offset: Int)

    fun reset(offset: Int)

    fun resampled(timeframe: Timeframe): CandleSeries

    companion object {

        operator fun invoke(
            inputSeries: CandleSeries,
            initialIndex: Int,
            currentOffset: Int,
            isSessionStart: (CandleSeries, Int) -> Boolean,
        ): BarReplaySession = BarReplaySessionImpl(
            inputSeries = inputSeries,
            initialIndex = initialIndex,
            currentOffset = currentOffset,
            isSessionStart = isSessionStart,
        )
    }
}

private class BarReplaySessionImpl(
    override val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    private val isSessionStart: (CandleSeries, Int) -> Boolean,
) : BarReplaySession {

    private val _replaySeries = MutableCandleSeries(
        initial = inputSeries.subList(0, initialIndex + currentOffset),
        timeframe = inputSeries.timeframe,
    )

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

    override fun reset(offset: Int) {

        _replaySeries.removeLast(offset)

        resamplings.values.forEach { resampledSeries ->

            // Resample initial candles again
            val initialResampledCandles = initialResampleCandles(resampledSeries.timeframe!!)

            // Remove extra candles
            resampledSeries.removeLast(resampledSeries.size - initialResampledCandles.size)

            // Last candle could be resampled with more candles than initialized with.
            // Replace last candle if that's the case.
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
