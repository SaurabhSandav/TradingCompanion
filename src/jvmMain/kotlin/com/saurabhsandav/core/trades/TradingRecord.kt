package com.saurabhsandav.core.trades

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter
import java.util.*

internal class TradingRecord(
    recordPath: String,
    onTradeCountsUpdated: suspend (tradeCount: Int, tradeCountOpen: Int) -> Unit,
) {

    private val tradesDB: TradesDB = run {

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$recordPath/Trades.db",
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = TradesDB.Schema,
            callbacks = arrayOf(
                migrationAfterV1,
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
        tradesDB = tradesDB,
        onTradesUpdated = {

            val (totalCount, openCount) = tradesDB.tradeQueries
                .getTotalAndOpenCount()
                .executeAsOne()
                .run { totalCount.toInt() to openCount.toInt() }

            onTradeCountsUpdated(totalCount, openCount)
        },
    )

    val trades = Trades(recordPath, tradesDB, executions)

    val sizingTrades = SizingTrades(tradesDB)

    private companion object {

        val migrationAfterV1 = AfterVersion(1) { driver ->

            val transacter = object : TransacterImpl(driver) {}

            transacter.transaction {

                // Set farthest stop as primary
                driver.execute(
                    identifier = null,
                    sql = """
                    |UPDATE TradeStop AS ts
                    |SET isPrimary = TRUE 
                    |WHERE price = (
                    |  SELECT TradeStop.price
                    |  FROM TradeStop
                    |  INNER JOIN Trade ON TradeStop.tradeId = Trade.id
                    |  WHERE Trade.id = ts.tradeId
                    |  ORDER BY IIF(Trade.side = 'long', 1, -1.0) * CAST(TradeStop.price AS REAL)
                    |  LIMIT 1
                    |);
                    """.trimMargin(),
                    parameters = 0,
                )

                // Set closest target as primary
                driver.execute(
                    identifier = null,
                    sql = """
                    |UPDATE TradeTarget AS tt
                    |SET isPrimary = TRUE 
                    |WHERE price = (
                    |  SELECT TradeTarget.price
                    |  FROM TradeTarget
                    |  INNER JOIN Trade ON TradeTarget.tradeId = Trade.id
                    |  WHERE Trade.id = tt.tradeId
                    |  ORDER BY IIF(Trade.side = 'long', 1, -1.0) * CAST(TradeTarget.price AS REAL)
                    |  LIMIT 1
                    |);
                    """.trimMargin(),
                    parameters = 0,
                )
            }
        }
    }
}
