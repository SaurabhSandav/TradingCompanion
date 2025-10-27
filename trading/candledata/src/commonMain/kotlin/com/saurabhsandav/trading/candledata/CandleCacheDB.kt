package com.saurabhsandav.trading.candledata

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.kbigdecimal.toDouble
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.candledata.db.CandleQueriesCollection
import com.saurabhsandav.trading.candledata.migrations.migrationAfterV1
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Properties
import kotlin.coroutines.CoroutineContext
import kotlin.time.Instant

class CandleCacheDB(
    private val coroutineContext: CoroutineContext,
    dbUrl: String = JdbcSqliteDriver.IN_MEMORY,
) : CandleCache {

    private val driver = JdbcSqliteDriver(
        url = dbUrl,
        properties = Properties().apply { put("foreign_keys", "true") },
        callbacks = listOf(migrationAfterV1).toTypedArray(),
        schema = CandleDB.Schema,
    )

    private val candleDB: CandleDB = run {

        CandleDB(
            driver = driver,
            CheckedRangeAdapter = CheckedRange.Adapter(
                fromEpochSecondsAdapter = InstantLongColumnAdapter,
                toEpochSecondsAdapter = InstantLongColumnAdapter,
            ),
        )
    }

    private val candleQueriesCollection = CandleQueriesCollection(driver = driver)

    override suspend fun saveCheckedRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ) = withContext(coroutineContext) {

        // Assumes calling code downloads candles in a single expanding range without any gaps

        val checkedRangeQueries = candleDB.checkedRangeQueries
        val currentRange = getCheckedRange(symbolId, timeframe)

        // Expand range on either side
        val minFrom = minOf(from, currentRange?.start ?: from)
        val maxTo = maxOf(to, currentRange?.endInclusive ?: to)

        val tableName = candleQueriesCollection.getTableName(symbolId, timeframe)

        checkedRangeQueries.insert(tableName, minFrom, maxTo)
    }

    override suspend fun getCheckedRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
    ): ClosedRange<Instant>? = withContext(coroutineContext) {

        val checkedRangeQueries = candleDB.checkedRangeQueries

        val tableName = candleQueriesCollection.getTableName(symbolId, timeframe)

        return@withContext checkedRangeQueries
            .get(tableName) { _, start, end -> start..end }
            .executeAsOneOrNull()
    }

    override suspend fun replace(
        symbolId: SymbolId,
        timeframe: Timeframe,
        interval: ClosedRange<Instant>,
        new: List<Candle>,
    ) = withContext(coroutineContext) {

        val candlesQueries = candleQueriesCollection.getOrCreate(symbolId, timeframe)

        candlesQueries.transaction {

            candlesQueries.delete(
                from = interval.start,
                to = interval.endInclusive,
            )

            new.forEach { candle ->

                candlesQueries.insert(
                    epochSeconds = candle.openInstant.epochSeconds,
                    open = candle.open.toString(),
                    high = candle.high.toString(),
                    low = candle.low.toString(),
                    close = candle.close.toString(),
                    volume = candle.volume.toDouble().toLong(),
                )
            }
        }
    }

    override fun getCountInRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Flow<Long> = flow {

        candleQueriesCollection
            .getOrFail(symbolId, timeframe)
            .getCountInRange(from.epochSeconds, to.epochSeconds)
            .asFlow()
            .mapToOne(coroutineContext)
            .emitInto(this)
    }

    override fun getInstantBeforeByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        before: Instant,
        count: Int,
    ): Flow<Instant?> = flow {

        candleQueriesCollection
            .getOrFail(symbolId, timeframe)
            .getInstantBeforeByCount(
                before = before.epochSeconds,
                count = count.toLong(),
                mapper = Instant.Companion::fromEpochSeconds,
            )
            .asFlow()
            .mapToOneOrNull(coroutineContext)
            .emitInto(this)
    }

    override fun getInstantAfterByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        after: Instant,
        count: Int,
    ): Flow<Instant?> = flow {

        candleQueriesCollection
            .getOrFail(symbolId, timeframe)
            .getInstantAfterByCount(
                after = after.epochSeconds,
                count = count.toLong(),
                mapper = Instant.Companion::fromEpochSeconds,
            )
            .asFlow()
            .mapToOneOrNull(coroutineContext)
            .emitInto(this)
    }

    override fun fetchRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        includeFromCandle: Boolean,
    ): Flow<List<Candle>> = flow {

        val candlesQueries = candleQueriesCollection.getOrFail(symbolId, timeframe)

        val mapper: (Long, String, String, String, String, Long) -> Candle =
            { epochSeconds, open, high, low, close, volume ->
                Candle(
                    openInstant = Instant.fromEpochSeconds(epochSeconds),
                    open = open.toKBigDecimal(),
                    high = high.toKBigDecimal(),
                    low = low.toKBigDecimal(),
                    close = close.toKBigDecimal(),
                    volume = volume.toKBigDecimal(),
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
        }.asFlow().mapToList(coroutineContext).emitInto(this)
    }

    override suspend fun getCountAt(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
    ): CandleCache.CountRange? = withContext(coroutineContext) {

        val candlesQueries = candleQueriesCollection.getOrFail(symbolId, timeframe)
        val result = candlesQueries
            .getEpochSecondsAndCountAt(at.epochSeconds)
            .executeAsOneOrNull()

        return@withContext when (result) {
            null -> null
            else -> CandleCache.CountRange(
                firstCandleInstant = result.firstCandleEpochSeconds?.let(Instant::fromEpochSeconds),
                beforeCount = result.beforeCount,
                lastCandleInstant = result.lastCandleEpochSeconds?.let(Instant::fromEpochSeconds),
                afterCount = result.afterCount,
                atCandleExists = result.atCandleExists,
            )
        }
    }

    override fun getBefore(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        return flow {

            val candlesQueries = candleQueriesCollection.getOrFail(symbolId, timeframe)

            candlesQueries.getCountBefore(
                at = at.epochSeconds,
                count = count.toLong(),
                includeAt = includeAt,
            ) { epochSeconds, open, high, low, close, volume ->
                Candle(
                    Instant.fromEpochSeconds(epochSeconds),
                    open.toKBigDecimal(),
                    high.toKBigDecimal(),
                    low.toKBigDecimal(),
                    close.toKBigDecimal(),
                    volume.toKBigDecimal(),
                )
            }.asFlow().mapToList(coroutineContext).emitInto(this)
        }
    }

    override fun getAfter(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Flow<List<Candle>> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        return flow {

            val candlesQueries = candleQueriesCollection.getOrFail(symbolId, timeframe)

            candlesQueries.getCountAfter(
                at = at.epochSeconds,
                count = count.toLong(),
                includeAt = includeAt,
            ) { epochSeconds, open, high, low, close, volume ->
                Candle(
                    Instant.fromEpochSeconds(epochSeconds),
                    open.toKBigDecimal(),
                    high.toKBigDecimal(),
                    low.toKBigDecimal(),
                    close.toKBigDecimal(),
                    volume.toKBigDecimal(),
                )
            }.asFlow().mapToList(coroutineContext).emitInto(this)
        }
    }

    private suspend inline fun <T> Flow<T>.emitInto(collector: FlowCollector<T>) = collector.emitAll(this)
}
