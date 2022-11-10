package trading.barreplay

import trading.*

interface ReplaySession {

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
        ): ReplaySession = ReplaySessionImpl(
            inputSeries = inputSeries,
            initialIndex = initialIndex,
            currentOffset = currentOffset,
            isSessionStart = isSessionStart,
        )
    }
}

private class ReplaySessionImpl(
    override val inputSeries: CandleSeries,
    private val initialIndex: Int,
    currentOffset: Int,
    private val isSessionStart: (CandleSeries, Int) -> Boolean,
) : ReplaySession {

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

            // If session start, Add candle, else, Resample already added candle
            val resampledCandle = when {
                isSessionStart(replaySeries, replaySeries.lastIndex) -> candle
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
            val initialResampledCandles = replaySeries.getCandlesBySession(isSessionStart).map { candles ->
                candles.reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
            }

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

    override fun resampled(timeframe: Timeframe): CandleSeries = resamplings.getOrPut(timeframe) {
        MutableCandleSeries(
            initial = replaySeries.getCandlesBySession(isSessionStart).map { candles ->
                candles.reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
            },
            timeframe = Timeframe.D1,
        )
    }.asCandleSeries()

    private fun CandleSeries.getCandlesBySession(
        isSessionStart: (CandleSeries, Int) -> Boolean,
    ): List<List<Candle>> {

        // If no candles then no candles
        if (isEmpty()) return emptyList()

        val result = mutableListOf<List<Candle>>()

        var currentSessionStartIndex = 0

        // Add a list of all candles for every session to result
        indices.forEach { index ->

            // If it's session start and index is 0, there is no session before it to add
            if (isSessionStart(this, index) && index != 0) {

                // Add previous session
                result.add(subList(currentSessionStartIndex, index))

                // Remember current session start
                currentSessionStartIndex = index
            }
        }

        // Add candles for last session
        result.add(subList(currentSessionStartIndex, size))

        return result
    }

    private fun Candle.resample(newCandle: Candle): Candle = copy(
        high = if (high > newCandle.high) high else newCandle.high,
        low = if (low < newCandle.low) low else newCandle.low,
        close = newCandle.close,
        volume = volume + newCandle.volume,
    )
}
