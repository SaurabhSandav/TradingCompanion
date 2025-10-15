package com.saurabhsandav.trading.record.testdata

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
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

class CommonExitEntryTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 150.toKBigDecimal(),
            lots = 150,
            side = TradeExecutionSide.Sell,
            price = 1010.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #1 Close, Trade #2 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 250.toKBigDecimal(),
            lots = 250,
            side = TradeExecutionSide.Buy,
            price = 1020.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 11, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 100.toKBigDecimal(),
            lots = 100,
            side = TradeExecutionSide.Sell,
            price = 1040.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 12, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),
    )

    fun trades(index: Int): List<Trade> {

        return when (index) {
            0 -> firstExecutionTrades
            1 -> secondExecutionTrades
            2 -> thirdExecutionTrades
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
            closedQuantity = KBigDecimal.Zero,
            lots = executions[0].lots,
            closedLots = 0,
            side = TradeSide.Short,
            averageEntry = executions[0].price,
            entryTimestamp = executions[0].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = KBigDecimal.Zero,
            fees = KBigDecimal.Zero,
            netPnl = KBigDecimal.Zero,
            isClosed = false,
        ),
    )

    private val secondExecutionTrades = listOf(
        Trade(
            id = TradeId(-1),
            brokerId = executions[0].brokerId,
            instrument = executions[0].instrument,
            symbolId = executions[0].symbolId,
            quantity = executions[1].quantity - executions[0].quantity,
            closedQuantity = KBigDecimal.Zero,
            lots = executions[1].lots - executions[0].lots,
            closedLots = 0,
            side = TradeSide.Long,
            averageEntry = executions[1].price,
            entryTimestamp = executions[1].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = KBigDecimal.Zero,
            fees = KBigDecimal.Zero,
            netPnl = KBigDecimal.Zero,
            isClosed = false,
        ),
        firstExecutionTrades[0].copy(
            closedQuantity = executions[0].quantity,
            closedLots = executions[0].lots,
            averageExit = executions[1].price,
            exitTimestamp = executions[1].timestamp,
            pnl = (-1500).toKBigDecimal(),
            fees = "10".toKBigDecimal(),
            netPnl = "-1510".toKBigDecimal(),
            isClosed = true,
        ),
    )

    private val thirdExecutionTrades = listOf(
        secondExecutionTrades[0].copy(
            quantity = executions[2].quantity,
            closedQuantity = executions[2].quantity,
            lots = executions[2].lots,
            closedLots = executions[2].lots,
            averageExit = executions[2].price,
            exitTimestamp = executions[2].timestamp,
            pnl = 2000.toKBigDecimal(),
            fees = "10".toKBigDecimal(),
            netPnl = "1990".toKBigDecimal(),
            isClosed = true,
        ),
        secondExecutionTrades[1],
    )
}
