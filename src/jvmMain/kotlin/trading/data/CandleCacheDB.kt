package trading.data

import AppModule
import com.saurabhsandav.core.CandleDB
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import trading.Candle
import trading.Timeframe
import utils.AppPaths

class CandleDBCollection {

    private val dbMap = mutableMapOf<String, CandleDB>()

    fun get(symbol: String, timeframe: Timeframe): CandleDB {

        val dbName = "${symbol}_${timeframe.seconds}"

        return dbMap.getOrPut(dbName) {
            val driver = JdbcSqliteDriver("jdbc:sqlite:${AppPaths.getAppDataPath()}/Candles/${dbName}.db")
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
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ) = withContext(Dispatchers.IO) {

        // Assumes calling code downloads candles in a single expanding range without any gaps

        val checkedRangeQueries = candleDBCollection.get(symbol, timeframe).checkedRangeQueries
        val currentRange = getCachedRange(symbol, timeframe)

        // Expand range on either side
        val minFrom = minOf(from, currentRange?.start ?: from)
        val maxTo = maxOf(to, currentRange?.endInclusive ?: to)

        checkedRangeQueries.insert(minFrom.epochSeconds, maxTo.epochSeconds)
    }

    override suspend fun getCachedRange(
        symbol: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>? {

        val checkedRangeQueries = candleDBCollection.get(symbol, timeframe).checkedRangeQueries

        return checkedRangeQueries
            .get { _, start, end -> Instant.fromEpochSeconds(start)..Instant.fromEpochSeconds(end) }
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .first()
    }

    override suspend fun fetchRange(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): List<Candle> {

        val candlesQueries = candleDBCollection.get(symbol, timeframe).candlesQueries

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
        symbol: String,
        timeframe: Timeframe,
        at: Instant,
    ): CandleCache.CountRange? {

        val candlesQueries = candleDBCollection.get(symbol, timeframe).candlesQueries
        val result = candlesQueries.getEpochSecondsAndCountAt(at.epochSeconds).asFlow().mapToList(Dispatchers.IO).first()

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

    override suspend fun fetchByCount(
        symbol: String,
        timeframe: Timeframe,
        at: Instant,
        before: Int,
        after: Int,
    ): List<Candle> {

        require(before > 0 || after > 0) { "CandleCacheDB: Both before and after cannot be 0 or less than 0" }

        val candlesQueries = candleDBCollection.get(symbol, timeframe).candlesQueries

        return candlesQueries.getAtByCount(
            at = at.epochSeconds,
            before = before.coerceAtLeast(0).toLong(),
            after = after.coerceAtLeast(0).toLong(),
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
        symbol: String,
        timeframe: Timeframe,
        candles: List<Candle>,
    ) = withContext(Dispatchers.IO) {

        val candlesQueries = candleDBCollection.get(symbol, timeframe).candlesQueries

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
