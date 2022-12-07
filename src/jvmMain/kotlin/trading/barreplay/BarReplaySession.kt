package trading.barreplay

import trading.CandleSeries

interface BarReplaySession {

    val inputSeries: CandleSeries

    val replaySeries: CandleSeries

    fun addCandle(offset: Int)

    fun addCandle(offset: Int, candleState: BarReplay.CandleState)

    fun reset()
}

enum class CandleUpdateType {
    FullBar,
    OHLC;
}
