package com.saurabhsandav.core.trading.data

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.utils.AppPaths
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class CandleDBCollection {

    private val dbMap = mutableMapOf<String, CandleDB>()

    fun get(ticker: String, timeframe: Timeframe): CandleDB {

        val candlesFolderPath = Path("${AppPaths.getAppDataPath()}/Candles").createDirectories()

        val dbName = "${ticker}_${timeframe.seconds}"

        return dbMap.getOrPut(dbName) {
            val driver = JdbcSqliteDriver("jdbc:sqlite:$candlesFolderPath/${dbName}.db")
            CandleDB.Schema.create(driver)
            CandleDB(driver = driver)
        }
    }
}

internal class CandleCacheDB(
    appModule: AppModule,
    private val candleDBCollection: CandleDBCollection = appModule.candleDBCollection,
) : CandleCache {

    override suspend fun saveCheckedRange(
        ticker: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ) = withContext(Dispatchers.IO) {

        // Assumes calling code downloads candles in a single expanding range without any gaps

        val checkedRangeQueries = candleDBCollection.get(ticker, timeframe).checkedRangeQueries
        val currentRange = getCachedRange(ticker, timeframe)

        // Expand range on either side
        val minFrom = minOf(from, currentRange?.start ?: from)
        val maxTo = maxOf(to, currentRange?.endInclusive ?: to)

        checkedRangeQueries.insert(minFrom.epochSeconds, maxTo.epochSeconds)
    }

    override suspend fun getCachedRange(
        ticker: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>? {

        val checkedRangeQueries = candleDBCollection.get(ticker, timeframe).checkedRangeQueries

        return checkedRangeQueries
            .get { _, start, end -> Instant.fromEpochSeconds(start)..Instant.fromEpochSeconds(end) }
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

        val candlesQueries = candleDBCollection.get(ticker, timeframe).candlesQueries

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

        val candlesQueries = candleDBCollection.get(ticker, timeframe).candlesQueries
        val result =
            candlesQueries.getEpochSecondsAndCountAt(at.epochSeconds).asFlow().mapToList(Dispatchers.IO).first()

        return when {
            result.size != 2 -> null
            else -> CandleCache.CountRange(
                firstCandleInstant = Instant.fromEpochSeconds(result[0].MIN ?: at.epochSeconds),
                beforeCount = result[0].COUNT,
                lastCandleInstant = Instant.fromEpochSeconds(result[1].MIN ?: at.epochSeconds),
                afterCount = result[1].COUNT,
            )
        }
    }

    override suspend fun getBefore(
        ticker: String,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): List<Candle> {

        require(count > 0) { "CandleCacheDB: count should be greater than 0" }

        val candlesQueries = candleDBCollection.get(ticker, timeframe).candlesQueries

        return candlesQueries.getCountBefore(
            at = at.epochSeconds,
            count = count.toLong(),
            includeAt = includeAt.toString(),
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

        val candlesQueries = candleDBCollection.get(ticker, timeframe).candlesQueries

        return candlesQueries.getCountAfter(
            at = at.epochSeconds,
            count = count.toLong(),
            includeAt = includeAt.toString(),
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

        val candlesQueries = candleDBCollection.get(ticker, timeframe).candlesQueries

        candlesQueries.transaction {
            candles.forEach { candle ->
                candlesQueries.insert(
                    epochSeconds = candle.openInstant.epochSeconds,
                    open_ = candle.open.toPlainString(),
                    high = candle.high.toPlainString(),
                    low = candle.low.toPlainString(),
                    close = candle.close.toPlainString(),
                    volume = candle.volume.toLong(),
                )
            }
        }
    }
}
