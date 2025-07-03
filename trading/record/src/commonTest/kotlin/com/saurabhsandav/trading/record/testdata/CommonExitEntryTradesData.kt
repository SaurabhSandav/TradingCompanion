package com.saurabhsandav.trading.record.testdata

import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradeExecution
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSide
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal

class CommonExitEntryTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 150.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 1010.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #1 Close, Trade #2 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 250.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 1020.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 11, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol3"),
            quantity = 100.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 1040.toBigDecimal(),
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
        Trade(
            id = TradeId(-1),
            brokerId = executions[0].brokerId,
            instrument = executions[0].instrument,
            symbolId = executions[0].symbolId,
            quantity = executions[1].quantity - executions[0].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = null,
            side = TradeSide.Long,
            averageEntry = executions[1].price,
            entryTimestamp = executions[1].timestamp,
            averageExit = null,
            exitTimestamp = null,
            pnl = BigDecimal.ZERO,
            fees = BigDecimal.ZERO,
            netPnl = BigDecimal.ZERO,
            isClosed = false,
        ),
        firstExecutionTrades[0].copy(
            closedQuantity = executions[0].quantity,
            averageExit = executions[1].price,
            exitTimestamp = executions[1].timestamp,
            pnl = (-1500).toBigDecimal(),
            fees = "67.56".toBigDecimal(),
            netPnl = "-1567.56".toBigDecimal(),
            isClosed = true,
        ),
    )

    private val thirdExecutionTrades = listOf(
        secondExecutionTrades[0].copy(
            quantity = executions[2].quantity,
            closedQuantity = executions[2].quantity,
            averageExit = executions[2].price,
            exitTimestamp = executions[2].timestamp,
            pnl = 2000.toBigDecimal(),
            fees = "49.44".toBigDecimal(),
            netPnl = "1950.56".toBigDecimal(),
            isClosed = true,
        ),
        secondExecutionTrades[1],
    )
}
