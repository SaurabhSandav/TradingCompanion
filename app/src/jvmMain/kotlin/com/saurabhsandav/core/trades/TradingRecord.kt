package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trades.migrations.migrationAfterV1
import com.saurabhsandav.core.trades.migrations.migrationAfterV2
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.DbUrlProvider
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter
import java.nio.file.Path
import java.util.*

internal class TradingRecord(
    appDispatchers: AppDispatchers,
    recordPath: Path,
    dbUrlProvider: DbUrlProvider,
    onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
) {

    private val tradesDB: TradesDB = run {

        val driver = JdbcSqliteDriver(
            url = dbUrlProvider.getTradingRecordDbUrl(recordPath),
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = TradesDB.Schema,
            callbacks = arrayOf(
                migrationAfterV1,
                migrationAfterV2,
            ),
        )

        TradesDB(
            driver = driver,
            SizingTradeAdapter = SizingTrade.Adapter(
                idAdapter = SizingTradeIdColumnAdapter,
                entryAdapter = BigDecimalColumnAdapter,
                stopAdapter = BigDecimalColumnAdapter,
            ),
            TradeAdapter = Trade.Adapter(
                idAdapter = TradeIdColumnAdapter,
                instrumentAdapter = Instrument.ColumnAdapter,
                quantityAdapter = BigDecimalColumnAdapter,
                closedQuantityAdapter = BigDecimalColumnAdapter,
                lotsAdapter = IntColumnAdapter,
                sideAdapter = TradeSide.ColumnAdapter,
                averageEntryAdapter = BigDecimalColumnAdapter,
                entryTimestampAdapter = InstantReadableColumnAdapter,
                averageExitAdapter = BigDecimalColumnAdapter,
                exitTimestampAdapter = InstantReadableColumnAdapter,
                pnlAdapter = BigDecimalColumnAdapter,
                feesAdapter = BigDecimalColumnAdapter,
                netPnlAdapter = BigDecimalColumnAdapter,
            ),
            TradeExecutionAdapter = TradeExecution.Adapter(
                idAdapter = TradeExecutionIdColumnAdapter,
                instrumentAdapter = Instrument.ColumnAdapter,
                quantityAdapter = BigDecimalColumnAdapter,
                lotsAdapter = IntColumnAdapter,
                sideAdapter = TradeExecutionSide.ColumnAdapter,
                priceAdapter = BigDecimalColumnAdapter,
                timestampAdapter = InstantReadableColumnAdapter,
            ),
            TradeStopAdapter = TradeStop.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                priceAdapter = BigDecimalColumnAdapter,
            ),
            TradeTargetAdapter = TradeTarget.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                priceAdapter = BigDecimalColumnAdapter,
            ),
            TradeAttachmentAdapter = TradeAttachment.Adapter(
                idAdapter = TradeAttachmentIdColumnAdapter,
            ),
            TradeNoteAdapter = TradeNote.Adapter(
                idAdapter = TradeNoteIdColumnAdapter,
                tradeIdAdapter = TradeIdColumnAdapter,
                addedAdapter = InstantReadableColumnAdapter,
                lastEditedAdapter = InstantReadableColumnAdapter,
            ),
            TradeTagAdapter = TradeTag.Adapter(
                idAdapter = TradeTagIdColumnAdapter,
                colorAdapter = IntColumnAdapter,
            ),
            TradeToExecutionMapAdapter = TradeToExecutionMap.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                executionIdAdapter = TradeExecutionIdColumnAdapter,
                overrideQuantityAdapter = BigDecimalColumnAdapter,
            ),
            TradeToAttachmentMapAdapter = TradeToAttachmentMap.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                attachmentIdAdapter = TradeAttachmentIdColumnAdapter,
            ),
            TradeToTagMapAdapter = TradeToTagMap.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                tagIdAdapter = TradeTagIdColumnAdapter,
            ),
            ReviewAdapter = Review.Adapter(
                idAdapter = ReviewIdColumnAdapter,
                tradeIdsAdapter = TradeIdListColumnAdapter,
                createdAdapter = InstantReadableColumnAdapter,
            ),
            TradeExcursionsAdapter = TradeExcursions.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                tradeMfePriceAdapter = BigDecimalColumnAdapter,
                tradeMfePnlAdapter = BigDecimalColumnAdapter,
                tradeMaePriceAdapter = BigDecimalColumnAdapter,
                tradeMaePnlAdapter = BigDecimalColumnAdapter,
                sessionMfePriceAdapter = BigDecimalColumnAdapter,
                sessionMfePnlAdapter = BigDecimalColumnAdapter,
                sessionMaePriceAdapter = BigDecimalColumnAdapter,
                sessionMaePnlAdapter = BigDecimalColumnAdapter,
            ),
        )
    }

    val executions = TradeExecutions(
        appDispatchers = appDispatchers,
        tradesDB = tradesDB,
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
        recordPath = recordPath,
        tradesDB = tradesDB,
        executions = executions,
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
