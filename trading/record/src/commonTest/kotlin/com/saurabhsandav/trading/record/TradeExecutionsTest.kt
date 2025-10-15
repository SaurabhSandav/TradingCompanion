package com.saurabhsandav.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.testdata.MultipleSymbolsInIntervalData
import com.saurabhsandav.trading.record.testdata.SimpleTradesData
import com.saurabhsandav.trading.record.utils.withoutNanoseconds
import com.saurabhsandav.trading.test.TestBrokerProvider
import com.saurabhsandav.trading.test.assertBDEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class TradeExecutionsTest {

    private val scope = TestScope()
    private val tradesDB = createTradesDB()
    private var executions = Executions(
        coroutineContext = StandardTestDispatcher(scope.testScheduler),
        tradesDB = tradesDB,
        attachmentsDir = null,
        brokerProvider = TestBrokerProvider,
        getSymbol = null,
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
                brokerId = BrokerId("EditedTestBroker"),
                instrument = Instrument.Options,
                symbolId = SymbolId("EditedTestSymbol"),
                quantity = 20.toKBigDecimal(),
                lots = 20,
                side = TradeExecutionSide.Sell,
                price = 200.toKBigDecimal(),
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
            brokerId = BrokerId("EditedTestBroker"),
            instrument = Instrument.Options,
            symbolId = SymbolId("EditedTestSymbol"),
            quantity = 20.toKBigDecimal(),
            lots = 20,
            side = TradeExecutionSide.Sell,
            price = 200.toKBigDecimal(),
            timestamp = currentTime,
        )

        val editedExecution = executions.getById(id).first()

        assertEquals("EditedTestBroker", editedExecution.brokerId.value)
        assertEquals(Instrument.Options, editedExecution.instrument)
        assertEquals("EditedTestSymbol", editedExecution.symbolId.value)
        assertBDEquals(20.toKBigDecimal(), editedExecution.quantity)
        assertEquals(20, editedExecution.lots)
        assertEquals(TradeExecutionSide.Sell, editedExecution.side)
        assertBDEquals(200.toKBigDecimal(), editedExecution.price)
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

        assertFailsWith<IllegalStateException>(message = "TradeExecution($id) not found") {
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
                .map { it.copy(id = TradeExecutionId(-1)) },
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
    fun getBySymbolInInterval() = scope.runTest {

        val data = MultipleSymbolsInIntervalData()

        executions.new(data.executions)

        // Trade #1 and #4 have "TestSymbol" symbol. Trade #4 should be ignored.

        @Suppress("ktlint:standard:range-spacing")
        val interval = LocalDateTime(2024, Month.MAY, 1, 0, 0).toInstant(TimeZone.UTC)..
            LocalDateTime(2024, Month.MAY, 2, 0, 0).toInstant(TimeZone.UTC)

        val executionsInInterval = executions.getBySymbolInInterval(
            symbolId = SymbolId("TestSymbol"),
            range = interval,
        ).first()
        assertEquals(data.trade1Executions, executionsInInterval.map { it.copy(id = TradeExecutionId(-1)) })
    }

    @Test
    fun getBySymbolAndTradeIdsInInterval() = scope.runTest {

        val data = MultipleSymbolsInIntervalData()

        executions.new(data.executions)

        // Trade #1, #4 and #5 have "TestSymbol" symbol. Trade #4 and #5 should be ignored.

        @Suppress("ktlint:standard:range-spacing")
        val interval1 = LocalDateTime(2024, Month.MAY, 1, 0, 0).toInstant(TimeZone.UTC)..
            LocalDateTime(2024, Month.MAY, 5, 0, 0).toInstant(TimeZone.UTC)
        val executionsInInterval1 = executions.getBySymbolAndTradeIdsInInterval(
            symbolId = SymbolId("TestSymbol"),
            ids = listOf(TradeId(1)),
            range = interval1,
        ).first()
        assertEquals(data.trade1Executions, executionsInInterval1.map { it.copy(id = TradeExecutionId(-1)) })

        // Trade #2 and #3 have "TestSymbol1" symbol. Trade #3 should be ignored.

        @Suppress("ktlint:standard:range-spacing")
        val interval2 = LocalDateTime(2024, Month.MAY, 2, 0, 0).toInstant(TimeZone.UTC)..
            LocalDateTime(2024, Month.MAY, 4, 0, 0).toInstant(TimeZone.UTC)
        val executionsInInterval2 = executions.getBySymbolAndTradeIdsInInterval(
            symbolId = SymbolId("TestSymbol1"),
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
            brokerId = execution.brokerId,
            instrument = execution.instrument,
            symbolId = execution.symbolId,
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

        return TradesDB(driver)
    }
}
