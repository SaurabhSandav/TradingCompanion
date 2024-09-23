package com.saurabhsandav.core.trades.testdata

import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradeExecution
import com.saurabhsandav.core.trades.model.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import java.time.Month

class CommonExitEntryTradesData {

    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker3",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker3",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker3",
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
            broker = executions[0].broker,
            instrument = executions[0].instrument,
            ticker = executions[0].ticker,
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
            broker = executions[0].broker,
            instrument = executions[0].instrument,
            ticker = executions[0].ticker,
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
            fees = "55.76".toBigDecimal(),
            netPnl = "-1555.76".toBigDecimal(),
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
            fees = "37.64".toBigDecimal(),
            netPnl = "1962.36".toBigDecimal(),
            isClosed = true,
        ),
        secondExecutionTrades[1],
    )
}
