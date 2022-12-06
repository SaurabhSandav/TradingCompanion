package trading.data

import AppModule
import com.saurabhsandav.core.CandleDB
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
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

    override suspend fun getAvailableCandleRange(
        symbol: String,
        timeframe: Timeframe,
    ): ClosedRange<Instant>? {

        val candlesQueries = candleDBCollection.get(symbol, timeframe).candlesQueries
        val result = candlesQueries.getRange().asFlow().mapToList(Dispatchers.IO).first()
            .map(Instant.Companion::fromEpochSeconds)

        return when {
            result.isEmpty() -> null
            else -> result[0]..result[1]
        }
    }

    override suspend fun fetch(
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

    override suspend fun writeCandles(
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
