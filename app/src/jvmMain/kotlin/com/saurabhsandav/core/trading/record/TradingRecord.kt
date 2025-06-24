package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV5
import com.saurabhsandav.core.utils.DbUrlProvider
import java.nio.file.Path
import java.util.Properties
import kotlin.coroutines.CoroutineContext

internal class TradingRecord(
    coroutineContext: CoroutineContext,
    recordPath: Path,
    dbUrlProvider: DbUrlProvider,
    onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
) {

    private val attachmentsPath = recordPath.resolve("attachments")

    private val tradesDB: TradesDB = run {

        val driver = JdbcSqliteDriver(
            url = dbUrlProvider.getTradingRecordDbUrl(recordPath),
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = TradesDB.Schema,
            callbacks = arrayOf(
                migrationAfterV1,
                migrationAfterV2,
                migrationAfterV5(attachmentsPath),
            ),
        )

        TradesDB(driver)
    }

    val executions = Executions(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
        attachmentsPath = attachmentsPath,
        onTradesUpdated = {

            val (totalCount, openCount) = tradesDB.tradeQueries
                .getTotalAndOpenCount()
                .executeAsOne()
                .run { totalCount.toInt() to openCount.toInt() }

            onTradeCountsUpdated(totalCount, openCount)
        },
    )

    val trades = Trades(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
        executions = executions,
    )

    val stops = Stops(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val targets = Targets(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val tags = Tags(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val notes = Notes(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val attachments = Attachments(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
        attachmentsPath = attachmentsPath,
    )

    val excursions = Excursions(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val reviews = Reviews(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )

    val sizingTrades = SizingTrades(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
    )
}
