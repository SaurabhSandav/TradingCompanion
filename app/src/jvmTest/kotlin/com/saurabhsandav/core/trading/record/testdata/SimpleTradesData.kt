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

class SimpleTradesData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker1",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker2",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker1",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker1",
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
            broker = "Finvasia",
            instrument = Instrument.Equity,
            ticker = "TestTicker1",
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
        firstExecutionTrades[0].copy(
            closedQuantity = executions[1].quantity,
            averageExit = executions[1].price,
            exitTimestamp = executions[1].timestamp,
            pnl = (-1000).toBigDecimal(),
            fees = "1.18".toBigDecimal(),
            netPnl = "-1001.18".toBigDecimal(),
            isClosed = true,
        ),
    )

    private val thirdExecutionTrades = listOf(
        Trade(
            id = TradeId(-1),
            broker = executions[2].broker,
            instrument = executions[2].instrument,
            ticker = executions[2].ticker,
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
            broker = executions[3].broker,
            instrument = executions[3].instrument,
            ticker = executions[3].ticker,
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
            fees = "0.51".toBigDecimal(),
            netPnl = "299.49".toBigDecimal(),
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
            fees = "2.395".toBigDecimal(),
            netPnl = "1047.6".toBigDecimal(),
            isClosed = true,
        ),
        secondExecutionTrades[0],
    )
}
