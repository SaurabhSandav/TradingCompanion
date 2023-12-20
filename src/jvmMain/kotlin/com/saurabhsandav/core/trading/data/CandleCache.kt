package com.saurabhsandav.core.trading.data

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.coroutines.flow.Flow
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

    fun getCountInRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long>

    fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        edgeCandlesInclusive: Boolean,
    ): Flow<List<Candle>>

    suspend fun getCountAt(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
    ): CountRange?

    fun getBefore(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>>

    fun getAfter(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>>

    suspend fun save(
        ticker: String,
        timeframe: Timeframe,
        candles: List<Candle>,
    )

    data class CountRange(
        val beforeCount: Long,
        val afterCount: Long,
        val firstCandleInstant: Instant?,
        val lastCandleInstant: Instant?,
        val atCandleExists: Boolean,
    )
}
