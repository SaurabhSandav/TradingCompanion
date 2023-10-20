package com.saurabhsandav.core.trading.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

internal class CandleCacheDB(
    private val candleDB: CandleDB,
    private val candleQueriesCollection: CandleQueriesCollection,
) : CandleCache {

    override suspend fun saveCheckedRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ) = withContext(Dispatchers.IO) {

        // Assumes calling code downloads candles in a single expanding range without any gaps

        val checkedRangeQueries = candleDB.checkedRangeQueries
        val currentRange = getCheckedRange(ticker, timeframe)

        // Expand range on either side
        val minFrom = minOf(from, currentRange?.start ?: from)
        val maxTo = maxOf(to, currentRange?.endInclusive ?: to)

        val tableName = candleQueriesCollection.getTableName(ticker, timeframe)

        checkedRangeQueries.insert(tableName, minFrom, maxTo)
    }

    override suspend fun getCheckedRange(
        ticker: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>? {

        val checkedRangeQueries = candleDB.checkedRangeQueries

        val tableName = candleQueriesCollection.getTableName(ticker, timeframe)

        return checkedRangeQueries
            .get(tableName) { _, start, end -> start..end }
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .first()
    }

    override suspend fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): List<Candle> {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        return candlesQueries.getInRange(
            from = from.epochSeconds,
            to = to.epochSeconds,
        ) { epochSeconds, open, high, low, close, volume ->
            Candle(
                Instant.fromEpochSeconds(epochSeconds),
                open.toBigDecimal(),
                high.toBigDecimal(),
                low.toBigDecimal(),
                close.toBigDecimal(),
                volume.toBigDecimal(),
            )
        }.asFlow().mapToList(Dispatchers.IO).first()
    }

    override suspend fun getCountAt(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
    ): CandleCache.CountRange? {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)
        val result = candlesQueries.getEpochSecondsAndCountAt(at.epochSeconds)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .first()
            ?: return null

        return CandleCache.CountRange(
            firstCandleInstant = result.firstCandleEpochSeconds?.let(Instant::fromEpochSeconds),
            beforeCount = result.beforeCount,
            lastCandleInstant = result.lastCandleEpochSeconds?.let(Instant::fromEpochSeconds),
            afterCount = result.afterCount,
            atCandleExists = result.atCandleExists,
        )
    }

    override suspend fun getBefore(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): List<Candle> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        return candlesQueries.getCountBefore(
            at = at.epochSeconds,
            count = count.toLong(),
            includeAt = includeAt,
        ) { epochSeconds, open, high, low, close, volume ->
            Candle(
                Instant.fromEpochSeconds(epochSeconds),
                open.toBigDecimal(),
                high.toBigDecimal(),
                low.toBigDecimal(),
                close.toBigDecimal(),
                volume.toBigDecimal(),
            )
        }.asFlow().mapToList(Dispatchers.IO).first()
    }

    override suspend fun getAfter(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): List<Candle> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        return candlesQueries.getCountAfter(
            at = at.epochSeconds,
            count = count.toLong(),
            includeAt = includeAt,
        ) { epochSeconds, open, high, low, close, volume ->
            Candle(
                Instant.fromEpochSeconds(epochSeconds),
                open.toBigDecimal(),
                high.toBigDecimal(),
                low.toBigDecimal(),
                close.toBigDecimal(),
                volume.toBigDecimal(),
            )
        }.asFlow().mapToList(Dispatchers.IO).first()
    }

    override suspend fun save(
        ticker: String,
        timeframe: Timeframe,
        candles: List<Candle>,
    ) = withContext(Dispatchers.IO) {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        candlesQueries.transaction {
            candles.forEach { candle ->
                candlesQueries.insert(
                    epochSeconds = candle.openInstant.epochSeconds,
                    open = candle.open.toPlainString(),
                    high = candle.high.toPlainString(),
                    low = candle.low.toPlainString(),
                    close = candle.close.toPlainString(),
                    volume = candle.volume.toLong(),
                )
            }
        }
    }
}
