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

class OverlappingTimeTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = 10.toKBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 100.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol2"),
            quantity = 25.toKBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 220.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 10, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #1 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = 10.toKBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 102.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 11, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = TestBroker.Id,
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol2"),
            quantity = 25.toKBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 225.toKBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 12, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),
    )

    fun trades(index: Int): List<Trade> {

        return when (index) {
            0 -> firstExecutionTrades
            1 -> secondExecutionTrades
            2 -> thirdExecutionTrades
            3 -> fourthExecutionTrades
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
            side = TradeSide.Long,
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
            brokerId = executions[1].brokerId,
            instrument = executions[1].instrument,
            symbolId = executions[1].symbolId,
            quantity = executions[1].quantity,
            closedQuantity = KBigDecimal.Zero,
            lots = executions[1].lots,
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
        firstExecutionTrades[0],
    )

    private val thirdExecutionTrades = listOf(
        secondExecutionTrades[0],
        firstExecutionTrades[0].copy(
            closedQuantity = executions[0].quantity,
            averageExit = executions[2].price,
            exitTimestamp = executions[2].timestamp,
            pnl = 20.toKBigDecimal(),
            fees = "0.606".toKBigDecimal(),
            netPnl = "19.39".toKBigDecimal(),
            isClosed = true,
        ),
    )

    private val fourthExecutionTrades = listOf(
        secondExecutionTrades[0].copy(
            closedQuantity = executions[1].quantity,
            side = TradeSide.Long,
            averageExit = executions[3].price,
            exitTimestamp = executions[3].timestamp,
            pnl = 125.toKBigDecimal(),
            fees = "3.3375".toKBigDecimal(),
            netPnl = "121.66".toKBigDecimal(),
            isClosed = true,
        ),
        thirdExecutionTrades[1],
    )
}
