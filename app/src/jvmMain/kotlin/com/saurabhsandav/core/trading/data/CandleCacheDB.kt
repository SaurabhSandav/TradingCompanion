package com.saurabhsandav.core.trading.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trading.core.Candle
import com.saurabhsandav.core.trading.core.Timeframe
import com.saurabhsandav.core.trading.data.db.CandleQueriesCollection
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal class CandleCacheDB(
    private val appDispatchers: AppDispatchers,
    private val candleDB: CandleDB,
    private val candleQueriesCollection: CandleQueriesCollection,
) : CandleCache {

    override suspend fun saveCheckedRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ) = withContext(appDispatchers.IO) {

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
            .mapToOneOrNull(appDispatchers.IO)
            .first()
    }

    override suspend fun replace(
        ticker: String,
        timeframe: Timeframe,
        interval: ClosedRange<Instant>,
        new: List<Candle>,
    ) = withContext(appDispatchers.IO) {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)

        candlesQueries.transaction {

            candlesQueries.delete(
                from = interval.start,
                to = interval.endInclusive,
            )

            new.forEach { candle ->

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

    override fun getCountInRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long> = flow {

        candleQueriesCollection
            .get(ticker, timeframe)
            .getCountInRange(from.epochSeconds, to.epochSeconds)
            .asFlow()
            .mapToOne(appDispatchers.IO)
            .emitInto(this)
    }

    override fun getInstantBeforeByCount(
        ticker: String,
        timeframe: Timeframe,
        before: Instant,
        count: Int,
    ): Flow<Instant?> = flow {

        candleQueriesCollection
            .get(ticker, timeframe)
            .getInstantBeforeByCount(
                before = before.epochSeconds,
                count = count.toLong(),
                mapper = Instant.Companion::fromEpochSeconds,
            )
            .asFlow()
            .mapToOneOrNull(appDispatchers.IO)
            .emitInto(this)
    }

    override fun getInstantAfterByCount(
        ticker: String,
        timeframe: Timeframe,
        after: Instant,
        count: Int,
    ): Flow<Instant?> = flow {

        candleQueriesCollection
            .get(ticker, timeframe)
            .getInstantAfterByCount(
                after = after.epochSeconds,
                count = count.toLong(),
                mapper = Instant.Companion::fromEpochSeconds,
            )
            .asFlow()
            .mapToOneOrNull(appDispatchers.IO)
            .emitInto(this)
    }

    override fun fetchRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        includeFromCandle: Boolean,
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

        when {
            includeFromCandle -> candlesQueries.getInRangeFromCandleInclusive(
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
        }.asFlow().mapToList(appDispatchers.IO).emitInto(this)
    }

    override suspend fun getCountAt(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
    ): CandleCache.CountRange? {

        val candlesQueries = candleQueriesCollection.get(ticker, timeframe)
        val result = candlesQueries.getEpochSecondsAndCountAt(at.epochSeconds)
            .asFlow()
            .mapToOneOrNull(appDispatchers.IO)
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

            candlesQueries.getCountBefore(
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
            }.asFlow().mapToList(appDispatchers.IO).emitInto(this)
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

            candlesQueries.getCountAfter(
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
            }.asFlow().mapToList(appDispatchers.IO).emitInto(this)
        }
    }
}
