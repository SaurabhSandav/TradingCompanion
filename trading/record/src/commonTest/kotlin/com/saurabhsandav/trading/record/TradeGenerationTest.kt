package com.saurabhsandav.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.testdata.CommonExitEntryTradesData
import com.saurabhsandav.trading.record.testdata.OverlappingTimeTradesData
import com.saurabhsandav.trading.record.testdata.SimpleTradesData
import com.saurabhsandav.trading.test.TestBrokerProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test

class TradeGenerationTest {

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
    fun `Sequential trades, multiple entries and exits`() = scope.runTest {

        val data = SimpleTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            executions.new(execution)

            assertTradeListEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList(),
                message = "Execution #$index",
            )
        }
    }

    @Test
    fun `Time overlapping`() = scope.runTest {

        val data = OverlappingTimeTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            executions.new(execution)

            assertTradeListEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList(),
                message = "Execution #$index",
            )
        }
    }

    @Test
    fun `Common Exit and Entry order`() = scope.runTest {

        val data = CommonExitEntryTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            executions.new(execution)

            assertTradeListEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList(),
                message = "Execution #$index",
            )
        }
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
