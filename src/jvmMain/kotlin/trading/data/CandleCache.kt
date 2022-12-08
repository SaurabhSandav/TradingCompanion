package trading.data

import kotlinx.datetime.Instant
import trading.Candle
import trading.Timeframe

interface CandleCache {

    suspend fun saveCheckedRange(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    )

    suspend fun getAvailableCandleRange(
        symbol: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>?

    suspend fun fetch(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): List<Candle>

    suspend fun writeCandles(
        symbol: String,
        timeframe: Timeframe,
        candles: List<Candle>,
    )
}
