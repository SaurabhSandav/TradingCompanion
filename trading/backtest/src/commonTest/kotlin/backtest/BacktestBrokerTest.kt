package com.saurabhsandav.trading.backtest

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.test.TestBroker
import com.saurabhsandav.trading.test.TestBrokerProvider
import com.saurabhsandav.trading.test.assertBDEquals
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class BacktestBrokerTest {

    @Test
    fun `Initial state`() {

        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(persistentListOf(), sut.orders.value)
        assertEquals(persistentListOf(), sut.executions.value)
        assertEquals(persistentListOf(), sut.positions.value)
    }

    @Test
    fun `Place an order`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(8000, sut.availableMargin)
        assertBDEquals(2000, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Open>(sut.orders.value.first().status)
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Place an order, execute`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(8_000, sut.availableMargin)
        assertBDEquals(2_000, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value.first().status)
        assertEquals(1, sut.executions.value.size)
        assertEquals(1, sut.positions.value.size)
    }

    @Test
    fun `Order rejection, margin shortfall`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 1000.toKBigDecimal(),
                lots = 1000,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Rejected>(sut.orders.value.first().status)
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Order rejection, less than minimum order value`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(
            account = account,
            brokerProvider = TestBrokerProvider,
            minimumOrderValue = 1_000.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 1.toKBigDecimal(),
                lots = 1,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Rejected>(sut.orders.value.first().status)
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Order Cancellation`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        val orderId = sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 210.toKBigDecimal(),
        )

        sut.cancelOrder(orderId)

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 215.toKBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Canceled>(sut.orders.value.first().status)
        assertEquals(
            expected = currentTime + 1.minutes,
            actual = (sut.orders.value.first().status as BacktestOrder.Status.Canceled).closedAt,
        )
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Already cancelled order should not be cancelled again`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        val orderId = sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 210.toKBigDecimal(),
        )

        sut.cancelOrder(orderId)

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 215.toKBigDecimal(),
        )

        sut.cancelOrder(orderId)

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Canceled>(sut.orders.value.first().status)
        assertEquals(
            expected = currentTime + 1.minutes,
            actual = (sut.orders.value.first().status as BacktestOrder.Status.Canceled).closedAt,
        )
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Profitable position`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(8_000, sut.availableMargin)
        assertBDEquals(2_000, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value.first().status)
        assertEquals(1, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value.first().price)
        assertEquals(1, sut.positions.value.size)
        assertBDEquals(50, sut.positions.value.first().pnl)
    }

    @Test
    fun `Unprofitable position`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals("7948.815", sut.availableMargin)
        assertBDEquals("2051.185", sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value.first().status)
        assertEquals(1, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value.first().price)
        assertEquals(1, sut.positions.value.size)
        assertBDEquals((-50), sut.positions.value.first().pnl)
    }

    @Test
    fun `Close position in profit`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 215.toKBigDecimal(),
        )

        assertBDEquals("10098.77", account.balance)
        assertBDEquals("10098.77", sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(2, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[1].status)
        assertEquals(2, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertBDEquals(210, sut.executions.value[1].price)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Close position in loss`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(account, TestBrokerProvider)

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 198.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 200.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        assertBDEquals("9948.815", account.balance)
        assertBDEquals("9948.815", sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(2, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[1].status)
        assertEquals(2, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertBDEquals(195, sut.executions.value[1].price)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Close position orders should ignore minimum order value`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(
            account = account,
            brokerProvider = TestBrokerProvider,
            minimumOrderValue = 500.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 198.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 200.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        assertBDEquals("9948.815", account.balance)
        assertBDEquals("9948.815", sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(2, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[1].status)
        assertEquals(2, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertBDEquals(195, sut.executions.value[1].price)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Auto cancel with ocoId`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(
            account = account,
            brokerProvider = TestBrokerProvider,
            minimumOrderValue = 500.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 198.toKBigDecimal(),
        )

        // Stop
        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toKBigDecimal()),
            ocoId = Unit,
        )

        // Target
        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toKBigDecimal()),
            ocoId = Unit,
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 200.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        assertBDEquals("9948.815", account.balance)
        assertBDEquals("9948.815", sut.availableMargin)
        assertBDEquals(KBigDecimal.Zero, sut.usedMargin)
        assertEquals(3, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[1].status)
        assertIs<BacktestOrder.Status.Canceled>(sut.orders.value[2].status)
        assertEquals(2, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertBDEquals(195, sut.executions.value[1].price)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Null ocoId shouldn't cancel other orders with null ocoId`() {

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(
            account = account,
            brokerProvider = TestBrokerProvider,
            minimumOrderValue = 500.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 198.toKBigDecimal(),
        )

        // Stop
        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toKBigDecimal()),
        )

        // Target
        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 10.toKBigDecimal(),
                lots = 10,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            symbolId = symbolId,
            price = 200.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        assertBDEquals("9948.815", account.balance)
        assertBDEquals("7848.815", sut.availableMargin)
        assertBDEquals(2100, sut.usedMargin)
        assertEquals(3, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[1].status)
        assertIs<BacktestOrder.Status.Open>(sut.orders.value[2].status)
        assertEquals(2, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertBDEquals(195, sut.executions.value[1].price)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Margin Call`() {

        var marginCalled = false

        val currentTime = Clock.System.now()
        val symbolId = SymbolId("NTPC")
        val account = BacktestAccount(10_000.toKBigDecimal())
        val sut = BacktestBroker(
            account = account,
            brokerProvider = TestBrokerProvider,
            minimumOrderValue = 500.toKBigDecimal(),
            onMarginCall = { marginCalled = true },
        )

        sut.newPrice(
            instant = currentTime,
            symbolId = symbolId,
            price = 205.toKBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                brokerId = TestBroker.Id,
                instrument = Instrument.Equity,
                symbolId = symbolId,
                quantity = 48.toKBigDecimal(),
                lots = 48,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toKBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            symbolId = symbolId,
            price = 195.toKBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            symbolId = symbolId,
            price = 190.toKBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals("-85.616", sut.availableMargin)
        assertBDEquals("10085.616", sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertEquals(1, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertEquals(1, sut.positions.value.size)
        assertTrue(marginCalled)
    }
}
