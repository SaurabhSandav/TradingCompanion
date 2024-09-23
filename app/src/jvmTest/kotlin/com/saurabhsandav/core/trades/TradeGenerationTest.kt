package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.trades.migrations.migrationAfterV1
import com.saurabhsandav.core.trades.migrations.migrationAfterV2
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.trades.testdata.CommonExitEntryTradesData
import com.saurabhsandav.core.trades.testdata.OverlappingTimeTradesData
import com.saurabhsandav.core.trades.testdata.SimpleTradesData
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeGenerationTest {

    private val scope = TestScope()
    private val tradesDB = createTradesDB()
    private var tradeExecutions = TradeExecutions(
        appDispatchers = FakeAppDispatchers(scope),
        tradesDB = tradesDB,
        onTradesUpdated = {},
    )

    @Test
    fun `Sequential trades, multiple entries and exits`() = scope.runTest {

        val data = SimpleTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            tradeExecutions.new(execution)

            assertEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList().map { it.copy(id = TradeId(-1)) },
                message = "Execution #$index",
            )
        }
    }

    @Test
    fun `Time overlapping`() = scope.runTest {

        val data = OverlappingTimeTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            tradeExecutions.new(execution)

            assertEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList().map { it.copy(id = TradeId(-1)) },
                message = "Execution #$index",
            )
        }
    }

    @Test
    fun `Common Exit and Entry order`() = scope.runTest {

        val data = CommonExitEntryTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            tradeExecutions.new(execution)

            assertEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList().map { it.copy(id = TradeId(-1)) },
                message = "Execution #$index",
            )
        }
    }

    private suspend fun TradeExecutions.new(execution: TradeExecution): TradeExecutionId {

        return new(
            broker = execution.broker,
            instrument = execution.instrument,
            ticker = execution.ticker,
            quantity = execution.quantity,
            lots = execution.lots,
            side = execution.side,
            price = execution.price,
            timestamp = execution.timestamp,
            locked = execution.locked,
        )
    }

    private fun createTradesDB(): TradesDB {

        val driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = TradesDB.Schema,
            callbacks = arrayOf(
                migrationAfterV1,
                migrationAfterV2,
            ),
        )

        return TradesDB(
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
}
