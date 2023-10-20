package com.saurabhsandav.core.trading.data

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.datetime.Instant

interface CandleCache {

    suspend fun saveCheckedRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    )

    suspend fun getCheckedRange(
        ticker: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>?

    suspend fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): List<Candle>

    suspend fun getCountAt(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
    ): CountRange?

    suspend fun getBefore(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): List<Candle>

    suspend fun getAfter(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): List<Candle>

    suspend fun save(
        ticker: String,
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
