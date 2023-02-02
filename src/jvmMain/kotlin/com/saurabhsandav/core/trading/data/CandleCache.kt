package com.saurabhsandav.core.trading.data

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.datetime.Instant

interface CandleCache {

    suspend fun saveCheckedRange(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    )

    suspend fun getCachedRange(
        symbol: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>?

    suspend fun fetchRange(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): List<Candle>

    suspend fun getCountAt(
        symbol: String,
        timeframe: Timeframe,
        at: Instant,
    ): CountRange?

    suspend fun fetchByCount(
        symbol: String,
        timeframe: Timeframe,
        at: Instant,
        before: Int,
        after: Int,
    ): List<Candle>

    suspend fun save(
        symbol: String,
        timeframe: Timeframe,
        candles: List<Candle>,
    )

    data class CountRange(
        val firstCandleInstant: Instant,
        val beforeCount: Long,
        val lastCandleInstant: Instant,
        val afterCount: Long,
    )
}
