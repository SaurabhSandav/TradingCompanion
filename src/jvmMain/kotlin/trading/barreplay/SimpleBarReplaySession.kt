package trading.barreplay

import subList
import trading.CandleSeries
import trading.MutableCandleSeries
import trading.asCandleSeries

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

    init {

        // If candle is not closed, a new partially formed candle needs to be added.
        if (currentCandleState != BarReplay.CandleState.Close)
            addCandle(currentOffset, currentCandleState)
    }

    override val replaySeries: CandleSeries = _replaySeries.asCandleSeries()

    override fun addCandle(offset: Int) {

        // Get candle as-is and add it to replay series
        _replaySeries.addCandle(inputSeries[initialIndex + offset])
    }

    override fun addCandle(offset: Int, candleState: BarReplay.CandleState) {

        // Get full closed candle
        val fullCandle = inputSeries[initialIndex + offset]

        // Simulate candle at given candle state
        val candle = fullCandle.atState(candleState)

        // Add candle to replay series
        _replaySeries.addCandle(candle)
    }

    override fun reset() {

        // Reset replaySeries to initial state
        _replaySeries.removeLast(_replaySeries.size - initialIndex)
    }
}
