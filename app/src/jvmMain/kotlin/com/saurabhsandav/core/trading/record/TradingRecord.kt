package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV5
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.DbUrlProvider
import java.nio.file.Path
import java.util.Properties

internal class TradingRecord(
    appDispatchers: AppDispatchers,
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
        appDispatchers = appDispatchers,
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
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
        executions = executions,
    )

    val stops = Stops(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val targets = Targets(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val tags = Tags(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val notes = Notes(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val attachments = Attachments(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
        attachmentsPath = attachmentsPath,
    )

    val excursions = Excursions(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val reviews = Reviews(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )

    val sizingTrades = SizingTrades(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
    )
}
