package com.saurabhsandav.core.trading.data

import com.saurabhsandav.core.trading.core.Candle
import com.saurabhsandav.core.trading.core.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

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

    suspend fun replace(
        ticker: String,
        timeframe: Timeframe,
        interval: ClosedRange<Instant>,
        new: List<Candle>,
    )

    fun getCountInRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long>

    fun getInstantBeforeByCount(
        ticker: String,
        timeframe: Timeframe,
        before: Instant,
        count: Int,
    ): Flow<Instant?>

    fun getInstantAfterByCount(
        ticker: String,
        timeframe: Timeframe,
        after: Instant,
        count: Int,
    ): Flow<Instant?>

    fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        includeFromCandle: Boolean,
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

    data class CountRange(
        val beforeCount: Long,
        val afterCount: Long,
        val firstCandleInstant: Instant?,
        val lastCandleInstant: Instant?,
        val atCandleExists: Boolean,
    )
}
