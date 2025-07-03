package com.saurabhsandav.trading.record.testdata

import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradeExecution
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.test.TestBroker
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal

class SimpleTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = BigDecimal.ONE,
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 1000.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 1, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #1 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = BigDecimal.ONE,
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 2000.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 1, 10, 45).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 50.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 10.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 10).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #3 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol2"),
            quantity = 7.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 36.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 20).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Add
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 50.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 15.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Remove
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 40.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 20.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 40).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 60.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 25.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 50).toInstant(TimeZone.UTC),
            locked = true,
        ),
    )

    fun trades(index: Int): List<Trade> {

        return when (index) {
            0 -> firstExecutionTrades
            1 -> secondExecutionTrades
            2 -> thirdExecutionTrades
            3 -> fourthExecutionTrades
            4 -> fifthExecutionTrades
            5 -> sixthExecutionTrades
            6 -> seventhExecutionTrades
            else -> error("Invalid TradeExecution index ($index)")
        }
    }

    private val firstExecutionTrades = listOf(
        Trade(
            id = TradeId(-1),
            brokerId = executions[0].brokerId,
            instrument = executions[0].instrument,
            symbolId = executions[0].symbolId,
            quantity = executions[0].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = executions[0].lots,
            side = TradeSide.Short,
            averageEntry = executions[0].price,
            entryTimestamp = executions[0].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = BigDecimal.ZERO,
            fees = BigDecimal.ZERO,
            netPnl = BigDecimal.ZERO,
            isClosed = false,
        ),
    )

    private val secondExecutionTrades = listOf(
        firstExecutionTrades[0].copy(
            closedQuantity = executions[1].quantity,
            averageExit = executions[1].price,
            exitTimestamp = executions[1].timestamp,
            pnl = (-1000).toBigDecimal(),
            fees = "0.9".toBigDecimal(),
            netPnl = "-1000.9".toBigDecimal(),
            isClosed = true,
        ),
    )

    private val thirdExecutionTrades = listOf(
        Trade(
            id = TradeId(-1),
            brokerId = executions[2].brokerId,
            instrument = executions[2].instrument,
            symbolId = executions[2].symbolId,
            quantity = executions[2].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = executions[2].lots,
            side = TradeSide.Long,
            averageEntry = executions[2].price,
            entryTimestamp = executions[2].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = BigDecimal.ZERO,
            fees = BigDecimal.ZERO,
            netPnl = BigDecimal.ZERO,
            isClosed = false,
        ),
        secondExecutionTrades[0],
    )

    private val fourthExecutionTrades = listOf(
        Trade(
            id = TradeId(-1),
            brokerId = executions[3].brokerId,
            instrument = executions[3].instrument,
            symbolId = executions[3].symbolId,
            quantity = executions[3].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = executions[3].lots,
            side = TradeSide.Short,
            averageEntry = executions[3].price,
            entryTimestamp = executions[3].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = BigDecimal.ZERO,
            fees = BigDecimal.ZERO,
            netPnl = BigDecimal.ZERO,
            isClosed = false,
        ),
        thirdExecutionTrades[0],
        secondExecutionTrades[0],
    )

    private val fifthExecutionTrades = listOf(
        fourthExecutionTrades[0],
        thirdExecutionTrades[0].copy(
            quantity = 100.toBigDecimal(),
            averageEntry = "12.5".toBigDecimal(),
        ),
        secondExecutionTrades[0],
    )

    private val sixthExecutionTrades = listOf(
        fourthExecutionTrades[0],
        fifthExecutionTrades[1].copy(
            closedQuantity = 40.toBigDecimal(),
            averageExit = executions[5].price,
            exitTimestamp = executions[5].timestamp,
            pnl = 300.toBigDecimal(),
            fees = "0.39".toBigDecimal(),
            netPnl = "299.61".toBigDecimal(),
        ),
        secondExecutionTrades[0],
    )

    private val seventhExecutionTrades = listOf(
        fourthExecutionTrades[0],
        fifthExecutionTrades[1].copy(
            closedQuantity = 100.toBigDecimal(),
            averageExit = 23.toBigDecimal(),
            exitTimestamp = executions[6].timestamp,
            pnl = 1050.toBigDecimal(),
            fees = "1.065".toBigDecimal(),
            netPnl = "1048.94".toBigDecimal(),
            isClosed = true,
        ),
        secondExecutionTrades[0],
    )
}
