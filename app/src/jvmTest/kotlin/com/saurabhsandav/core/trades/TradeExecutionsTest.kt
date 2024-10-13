package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.trades.migrations.migrationAfterV1
import com.saurabhsandav.core.trades.migrations.migrationAfterV2
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.trades.testdata.MultipleTickersInIntervalData
import com.saurabhsandav.core.trades.testdata.SimpleTradesData
import com.saurabhsandav.core.utils.BigDecimalColumnAdapter
import com.saurabhsandav.core.utils.InstantReadableColumnAdapter
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Month
import java.util.*
import kotlin.test.*

class TradeExecutionsTest {

    private val scope = TestScope()
    private val tradesDB = createTradesDB()
    private var executions = Executions(
        appDispatchers = FakeAppDispatchers(scope),
        tradesDB = tradesDB,
        onTradesUpdated = {},
    )

    @Test
    fun new() = scope.runTest {

        val data = SimpleTradesData()
        val execution = data.executions.first()
        val id = executions.new(execution)
        val insertedExecution = executions.getById(id).first()

        assertEquals(execution, insertedExecution.copy(id = TradeExecutionId(-1)))
    }

    @Test
    fun `Edit Locked`() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first())

        assertFailsWith<IllegalArgumentException>("TradeExecution($id) is locked and cannot be edited") {

            executions.edit(
                id = id,
                broker = "EditedTestBroker",
                instrument = Instrument.Options,
                ticker = "EditedTestTicker",
                quantity = 20.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
                price = 200.toBigDecimal(),
                timestamp = Clock.System.now(),
            )
        }
    }

    @Test
    fun `Edit Unlocked`() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first().copy(locked = false))
        val currentTime = Clock.System.now()

        // Edit
        executions.edit(
            id = id,
            broker = "EditedTestBroker",
            instrument = Instrument.Options,
            ticker = "EditedTestTicker",
            quantity = 20.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 200.toBigDecimal(),
            timestamp = currentTime,
        )

        val editedExecution = executions.getById(id).first()

        assertEquals("EditedTestBroker", editedExecution.broker)
        assertEquals(Instrument.Options, editedExecution.instrument)
        assertEquals("EditedTestTicker", editedExecution.ticker)
        assertEquals(20.toBigDecimal(), editedExecution.quantity)
        assertNull(editedExecution.lots)
        assertEquals(TradeExecutionSide.Sell, editedExecution.side)
        assertEquals(200.toBigDecimal(), editedExecution.price)
        assertEquals(currentTime.withoutNanoseconds(), editedExecution.timestamp)
        assertFalse(editedExecution.locked)
    }

    @Test
    fun `Delete Locked`() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first())

        assertFailsWith<IllegalArgumentException>(message = "TradeExecution(s) are locked and cannot be deleted") {
            executions.delete(ids = listOf(id))
        }
    }

    @Test
    fun `Delete Unlocked`() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first().copy(locked = false))

        // Delete
        executions.delete(ids = listOf(id))

        assertFailsWith<IllegalStateException>(message = "TradeExecution(${id}) not found") {
            executions.getById(id).first()
        }
    }

    @Test
    fun lock() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first().copy(locked = false))

        // Lock
        executions.lock(ids = listOf(id))

        assertTrue(executions.getById(id).first().locked)
    }

    @Test
    fun getById() = scope.runTest {

        val data = SimpleTradesData()
        val id = executions.new(data.executions.first())

        // Check existing
        executions.getById(id).first()

        // Check non-existent
        assertFailsWith<IllegalStateException>(message = "TradeExecution(12) not found") {
            executions.getById(TradeExecutionId(12)).first()
        }
    }

    @Test
    fun getCount() = scope.runTest {

        val data = SimpleTradesData()

        val beforeTodayCount = executions.getBeforeTodayCount()
        val todayCount = executions.getTodayCount()

        assertEquals(0, beforeTodayCount.first())
        assertEquals(0, todayCount.first())

        executions.new(data.executions)
        executions.new(data.executions.first().copy(timestamp = Clock.System.now()))

        assertEquals(data.executions.size.toLong(), beforeTodayCount.first())
        assertEquals(1, todayCount.first())
    }

    @Test
    fun getForTrade() = scope.runTest {

        val data = SimpleTradesData()

        executions.new(data.executions)

        // Trade #1
        assertEquals(
            expected = data.executions.take(2),
            actual = executions.getForTrade(TradeId(1))
                .first()
                .map { it.copy(id = TradeExecutionId(-1)) }
        )

        // Trade #2
        assertEquals(
            expected = data.executions.subList(2, 3) + data.executions.subList(4, 7),
            actual = executions.getForTrade(TradeId(2))
                .first()
                .map { it.copy(id = TradeExecutionId(-1)) },
        )

        // Trade #3
        assertEquals(
            expected = data.executions.subList(3, 4),
            actual = executions.getForTrade(TradeId(3))
                .first()
                .map { it.copy(id = TradeExecutionId(-1)) },
        )
    }

    @Test
    fun getByTickerInInterval() = scope.runTest {

        val data = MultipleTickersInIntervalData()

        executions.new(data.executions)

        // Trade #1 and #4 have "TestTicker" ticker. Trade #4 should be ignored.

        val interval = LocalDateTime(2024, Month.MAY, 1, 0, 0).toInstant(TimeZone.UTC)..
                LocalDateTime(2024, Month.MAY, 2, 0, 0).toInstant(TimeZone.UTC)

        val executionsInInterval = executions.getByTickerInInterval(
            ticker = "TestTicker",
            range = interval,
        ).first()
        assertEquals(data.trade1Executions, executionsInInterval.map { it.copy(id = TradeExecutionId(-1)) })
    }

    @Test
    fun getByTickerAndTradeIdsInInterval() = scope.runTest {

        val data = MultipleTickersInIntervalData()

        executions.new(data.executions)

        // Trade #1, #4 and #5 have "TestTicker" ticker. Trade #4 and #5 should be ignored.

        val interval1 = LocalDateTime(2024, Month.MAY, 1, 0, 0).toInstant(TimeZone.UTC)..
                LocalDateTime(2024, Month.MAY, 5, 0, 0).toInstant(TimeZone.UTC)
        val executionsInInterval1 = executions.getByTickerAndTradeIdsInInterval(
            ticker = "TestTicker",
            ids = listOf(TradeId(1)),
            range = interval1,
        ).first()
        assertEquals(data.trade1Executions, executionsInInterval1.map { it.copy(id = TradeExecutionId(-1)) })

        // Trade #2 and #3 have "TestTicker1" ticker. Trade #3 should be ignored.

        val interval2 = LocalDateTime(2024, Month.MAY, 2, 0, 0).toInstant(TimeZone.UTC)..
                LocalDateTime(2024, Month.MAY, 4, 0, 0).toInstant(TimeZone.UTC)
        val executionsInInterval2 = executions.getByTickerAndTradeIdsInInterval(
            ticker = "TestTicker1",
            ids = listOf(TradeId(2)),
            range = interval2,
        ).first()
        assertEquals(data.trade2Executions, executionsInInterval2.map { it.copy(id = TradeExecutionId(-1)) })
    }

    private suspend fun Executions.new(executions: List<TradeExecution>) {
        executions.forEach { execution -> new(execution) }
    }

    private suspend fun Executions.new(execution: TradeExecution): TradeExecutionId {

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
