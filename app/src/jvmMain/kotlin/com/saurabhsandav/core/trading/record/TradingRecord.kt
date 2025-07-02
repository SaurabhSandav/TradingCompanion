package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV5
import java.nio.file.Path
import java.util.Properties
import kotlin.coroutines.CoroutineContext

internal class TradingRecord(
    coroutineContext: CoroutineContext,
    onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
    dbUrl: String = JdbcSqliteDriver.IN_MEMORY,
    attachmentsDir: Path? = null,
) {

    private val tradesDB: TradesDB = run {

        val driver = JdbcSqliteDriver(
            url = dbUrl,
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = TradesDB.Schema,
            callbacks = listOfNotNull(
                migrationAfterV1,
                migrationAfterV2,
                attachmentsDir?.let(::migrationAfterV5),
            ).toTypedArray(),
        )

        TradesDB(driver)
    }

    val executions = Executions(
        coroutineContext = coroutineContext,
        tradesDB = tradesDB,
        attachmentsDir = attachmentsDir,
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

    private val _attachments = attachmentsDir?.let {

        Attachments(
            coroutineContext = coroutineContext,
            tradesDB = tradesDB,
            attachmentsDir = attachmentsDir,
        )
    }

    val attachments: Attachments
        get() = checkNotNull(_attachments) { "Attachments directory not provided" }

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
