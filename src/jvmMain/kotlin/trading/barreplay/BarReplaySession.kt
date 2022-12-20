package trading.barreplay

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import trading.CandleSeries

interface BarReplaySession {

    val inputSeries: CandleSeries

    val replaySeries: CandleSeries

    val replayTime: StateFlow<Instant>

    fun addCandle(offset: Int)

    fun addCandle(offset: Int, candleState: BarReplay.CandleState)

    fun reset()
}

enum class CandleUpdateType {
    FullBar,
    OHLC;
}
