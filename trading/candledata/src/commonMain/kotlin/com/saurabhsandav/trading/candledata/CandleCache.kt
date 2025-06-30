package com.saurabhsandav.trading.candledata

import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface CandleCache {

    suspend fun saveCheckedRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    )

    suspend fun getCheckedRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
    ): ClosedRange<Instant>?

    suspend fun replace(
        symbolId: SymbolId,
        timeframe: Timeframe,
        interval: ClosedRange<Instant>,
        new: List<Candle>,
    )

    fun getCountInRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long>

    fun getInstantBeforeByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        before: Instant,
        count: Int,
    ): Flow<Instant?>

    fun getInstantAfterByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        after: Instant,
        count: Int,
    ): Flow<Instant?>

    fun fetchRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        includeFromCandle: Boolean,
    ): Flow<List<Candle>>

    suspend fun getCountAt(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
    ): CountRange?

    fun getBefore(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>>

    fun getAfter(
        symbolId: SymbolId,
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
