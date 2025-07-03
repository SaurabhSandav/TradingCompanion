package com.saurabhsandav.trading.record.testdata

import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.TradeExecution
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal

class MultipleSymbolsInIntervalData {

    @Suppress("ktlint:standard:no-blank-line-in-list")
    val executions: List<TradeExecution> = listOf(

        // Trade #1 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
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
            brokerId = BrokerId("Finvasia"),
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
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 50.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 10.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 10).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Add
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 50.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 15.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 20).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #2 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 100.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 20.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 2, 11, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #3 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 2.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 500.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 4, 9, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #3 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol1"),
            quantity = 2.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 400.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 4, 10, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #4 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = BigDecimal.TEN,
            lots = null,
            side = TradeExecutionSide.Buy,
            price = 100.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 10, 0).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #4 Close
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = BigDecimal.TEN,
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 102.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 5, 11, 30).toInstant(TimeZone.UTC),
            locked = true,
        ),

        // Trade #5 Open
        TradeExecution(
            id = TradeExecutionId(-1),
            brokerId = BrokerId("Finvasia"),
            instrument = Instrument.Equity,
            symbolId = SymbolId("TestSymbol"),
            quantity = 7.toBigDecimal(),
            lots = null,
            side = TradeExecutionSide.Sell,
            price = 36.toBigDecimal(),
            timestamp = LocalDateTime(2024, Month.MAY, 6, 11, 20).toInstant(TimeZone.UTC),
            locked = true,
        ),
    )

    val trade1Executions = executions.take(2)
    val trade2Executions = executions.subList(2, 5)
}
