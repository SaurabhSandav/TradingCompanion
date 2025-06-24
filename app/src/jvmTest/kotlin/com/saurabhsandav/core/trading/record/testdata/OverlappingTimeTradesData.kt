package com.saurabhsandav.core.trading.record.testdata

import com.saurabhsandav.core.trading.record.Trade
import com.saurabhsandav.core.trading.record.TradeExecution
import com.saurabhsandav.core.trading.record.model.Instrument
import com.saurabhsandav.core.trading.record.model.TradeExecutionId
import com.saurabhsandav.core.trading.record.model.TradeExecutionSide
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.model.TradeSide
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import java.time.Month

class OverlappingTimeTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker",
            quantity = BigDecimal.TEN,
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 100.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker2",
            quantity = 25.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 220.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 10, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #1 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker",
            quantity = BigDecimal.TEN,
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 102.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 11, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker2",
            quantity = 25.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 225.toBigDecimal(),
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
            broker = executions[0].broker,
            instrument = executions[0].instrument,
            ticker = executions[0].ticker,
            quantity = executions[0].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = executions[0].lots,
            side = TradeSide.Long,
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
            broker = executions[1].broker,
            instrument = executions[1].instrument,
            ticker = executions[1].ticker,
            quantity = executions[1].quantity,
            closedQuantity = BigDecimal.ZERO,
            lots = executions[1].lots,
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
        firstExecutionTrades[0],
    )

    private val thirdExecutionTrades = listOf(
        secondExecutionTrades[0],
        firstExecutionTrades[0].copy(
            closedQuantity = executions[0].quantity,
            averageExit = executions[2].price,
            exitTimestamp = executions[2].timestamp,
            pnl = 20.toBigDecimal(),
            fees = "0.796".toBigDecimal(),
            netPnl = "19.2".toBigDecimal(),
            isClosed = true,
        ),
    )

    private val fourthExecutionTrades = listOf(
        secondExecutionTrades[0].copy(
            closedQuantity = executions[1].quantity,
            side = TradeSide.Long,
            averageExit = executions[3].price,
            exitTimestamp = executions[3].timestamp,
            pnl = 125.toBigDecimal(),
            fees = "5.3975".toBigDecimal(),
            netPnl = "119.6".toBigDecimal(),
            isClosed = true,
        ),
        thirdExecutionTrades[1],
    )
}
