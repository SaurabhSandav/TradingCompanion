package com.saurabhsandav.core.trading.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

    override fun getCountInRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long> = flow {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        val flow = candlesQueries.getCountInRange(from.epochSeconds, to.epochSeconds)
            .asFlow()
            .mapToOne(Dispatchers.IO)

        emitAll(flow)
    }

    override fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        edgeCandlesInclusive: Boolean,
    ): Flow<List<Candle>> = flow {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        val mapper: (Long, String, String, String, String, Long) -> Candle =
            { epochSeconds, open, high, low, close, volume ->
                Candle(
                    openInstant = Instant.fromEpochSeconds(epochSeconds),
                    open = open.toBigDecimal(),
                    high = high.toBigDecimal(),
                    low = low.toBigDecimal(),
                    close = close.toBigDecimal(),
                    volume = volume.toBigDecimal(),
                )
            }

        val flow = when {
            edgeCandlesInclusive -> candlesQueries.getInRangeEdgeCandlesInclusive(
                from = from.epochSeconds,
                to = to.epochSeconds,
                candleSeconds = timeframe.seconds - 1,
                mapper = mapper,
            )

            else -> candlesQueries.getInRange(
                from = from.epochSeconds,
                to = to.epochSeconds,
                mapper = mapper,
            )
        }.asFlow().mapToList(Dispatchers.IO)

        emitAll(flow)
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

    override fun getBefore(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        return flow {

            val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

            val flow = candlesQueries.getCountBefore(
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
            }.asFlow().mapToList(Dispatchers.IO)

            emitAll(flow)
        }
    }

    override fun getAfter(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        return flow {

            val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

            val flow = candlesQueries.getCountAfter(
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
            }.asFlow().mapToList(Dispatchers.IO)

            emitAll(flow)
        }
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
