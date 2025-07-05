package com.saurabhsandav.trading.candledata.db

import app.cash.sqldelight.db.SqlDriver
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CandleQueriesCollection(
    private val driver: SqlDriver,
) {

    private var nextIdentifierSeries = 0
    private val mutex = Mutex()
    private val queriesMap = mutableMapOf<String, CandlesQueries>()

    fun getTableName(
        symbolId: SymbolId,
        timeframe: Timeframe,
    ): String {

        val ticker = symbolId.value
        val needsPrefix = ticker.first().isDigit()
        val timeframeStr = timeframe.seconds.toString()
        val length = ticker.length + timeframeStr.length + if (needsPrefix) 1 else 0

        return buildString(length) {
            if (needsPrefix) append('_')
            ticker.forEach { char -> if (char in listOf('-', '&', ':')) append('_') else append(char) }
            append('_')
            append(timeframeStr)
        }
    }

    suspend fun get(
        symbolId: SymbolId,
        timeframe: Timeframe,
    ): CandlesQueries = mutex.withLock {

        val tableName = getTableName(symbolId, timeframe)

        return@withLock queriesMap.getOrPut(tableName) {

            driver.execute(
                identifier = null,
                sql = """
                    |CREATE TABLE IF NOT EXISTS $tableName (
                    |epochSeconds INTEGER NOT NULL PRIMARY KEY,
                    |open TEXT NOT NULL,
                    |high TEXT NOT NULL,
                    |low TEXT NOT NULL,
                    |close TEXT NOT NULL,
                    |volume INTEGER NOT NULL
                    |)
                """.trimMargin(),
                parameters = 0,
            )

            nextIdentifierSeries += 1

            CandlesQueries(
                driver = driver,
                tableName = tableName,
                identifierSeries = nextIdentifierSeries,
            )
        }
    }
}
