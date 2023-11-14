package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter
import java.util.*

internal class TradingRecord(
    recordPath: String,
) {

    private val tradesDB: TradesDB = run {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$recordPath/Trades.db",
            properties = Properties().apply { put("foreign_keys", "true") },
        )
        TradesDB.Schema.create(driver)
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
            TradeMfeMaeAdapter = TradeMfeMae.Adapter(
                tradeIdAdapter = TradeIdColumnAdapter,
                mfePriceAdapter = BigDecimalColumnAdapter,
                mfePnlAdapter = BigDecimalColumnAdapter,
                maePriceAdapter = BigDecimalColumnAdapter,
                maePnlAdapter = BigDecimalColumnAdapter,
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
            )
        )
    }

    val executions = TradeExecutionsRepo(tradesDB)

    val trades = TradesRepo(recordPath, tradesDB, executions)

    val sizingTrades = SizingTradesRepo(tradesDB)
}
