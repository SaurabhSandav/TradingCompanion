package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV1
import com.saurabhsandav.core.trading.record.migrations.migrationAfterV2
import com.saurabhsandav.core.trading.record.model.TradeExecutionId
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.testdata.CommonExitEntryTradesData
import com.saurabhsandav.core.trading.record.testdata.OverlappingTimeTradesData
import com.saurabhsandav.core.trading.record.testdata.SimpleTradesData
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeGenerationTest {

    private val scope = TestScope()
    private val tradesDB = createTradesDB()
    private var executions = Executions(
        appDispatchers = FakeAppDispatchers(scope),
        tradesDB = tradesDB,
        attachmentsPath = Path(""),
        onTradesUpdated = {},
    )

    @Test
    fun `Sequential trades, multiple entries and exits`() = scope.runTest {

        val data = SimpleTradesData()
        val allTrades = tradesDB.tradeQueries.getAll()

        data.executions.forEachIndexed { index, execution ->

            executions.new(execution)

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

            executions.new(execution)

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

            executions.new(execution)

            assertEquals(
                expected = data.trades(index),
                actual = allTrades.executeAsList().map { it.copy(id = TradeId(-1)) },
                message = "Execution #$index",
            )
        }
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

        return TradesDB(driver)
    }
}
