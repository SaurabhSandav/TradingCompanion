package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.trading.record.model.Instrument
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.test.assertBDEquals
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class BacktestBrokerTest {

    // TODO Replace hardcoded broker Finvasia with a fake broker

    @Test
    fun `Initial state`() {

        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
        assertEquals(persistentListOf(), sut.orders.value)
        assertEquals(persistentListOf(), sut.executions.value)
        assertEquals(persistentListOf(), sut.positions.value)
    }

    @Test
    fun `Place an order`() {

        val currentTime = Clock.System.now()
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 205.toBigDecimal(),
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 1000.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Rejected>(sut.orders.value.first().status)
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Order rejection, less than minimum order value`() {

        val currentTime = Clock.System.now()
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(
            account = account,
            minimumOrderValue = 1_000.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 1.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Rejected>(sut.orders.value.first().status)
        assertEquals(0, sut.executions.value.size)
        assertEquals(0, sut.positions.value.size)
    }

    @Test
    fun `Order Cancellation`() {

        val currentTime = Clock.System.now()
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        val orderId = sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 210.toBigDecimal(),
        )

        sut.cancelOrder(orderId)

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 215.toBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        val orderId = sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 210.toBigDecimal(),
        )

        sut.cancelOrder(orderId)

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 215.toBigDecimal(),
        )

        sut.cancelOrder(orderId)

        assertBDEquals(10_000, account.balance)
        assertBDEquals(10_000, sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 205.toBigDecimal(),
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals("7948.44", sut.availableMargin)
        assertBDEquals("2051.56", sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 215.toBigDecimal(),
        )

        assertBDEquals("10097.38", account.balance)
        assertBDEquals("10097.38", sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(account)

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 198.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 200.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        assertBDEquals("9948.44", account.balance)
        assertBDEquals("9948.44", sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(
            account = account,
            minimumOrderValue = 500.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 198.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 200.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        assertBDEquals("9948.44", account.balance)
        assertBDEquals("9948.44", sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(
            account = account,
            minimumOrderValue = 500.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 198.toBigDecimal(),
        )

        // Stop
        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toBigDecimal()),
            ocoId = Unit,
        )

        // Target
        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toBigDecimal()),
            ocoId = Unit,
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 200.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        assertBDEquals("9948.44", account.balance)
        assertBDEquals("9948.44", sut.availableMargin)
        assertBDEquals(BigDecimal.ZERO, sut.usedMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(
            account = account,
            minimumOrderValue = 500.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 198.toBigDecimal(),
        )

        // Stop
        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = StopMarket(195.toBigDecimal()),
        )

        // Target
        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 10.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Sell,
            ),
            executionType = Limit(210.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 2.minutes,
            ticker = ticker,
            price = 200.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        assertBDEquals("9948.44", account.balance)
        assertBDEquals("7848.44", sut.availableMargin)
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
        val ticker = "NTPC"
        val account = BacktestAccount(10_000.toBigDecimal())
        val sut = BacktestBroker(
            account = account,
            minimumOrderValue = 500.toBigDecimal(),
            onMarginCall = { marginCalled = true },
        )

        sut.newPrice(
            instant = currentTime,
            ticker = ticker,
            price = 205.toBigDecimal(),
        )

        sut.newOrder(
            params = BacktestOrder.Params(
                broker = "Finvasia",
                instrument = Instrument.Equity,
                ticker = ticker,
                quantity = 48.toBigDecimal(),
                lots = null,
                side = TradeExecutionSide.Buy,
            ),
            executionType = Limit(200.toBigDecimal()),
        )

        sut.newPrice(
            instant = currentTime + 1.minutes,
            ticker = ticker,
            price = 195.toBigDecimal(),
        )

        sut.newPrice(
            instant = currentTime + 3.minutes,
            ticker = ticker,
            price = 190.toBigDecimal(),
        )

        assertBDEquals(10_000, account.balance)
        assertBDEquals("-89.42", sut.availableMargin)
        assertBDEquals("10089.42", sut.usedMargin)
        assertEquals(1, sut.orders.value.size)
        assertIs<BacktestOrder.Status.Executed>(sut.orders.value[0].status)
        assertEquals(1, sut.executions.value.size)
        assertBDEquals(200, sut.executions.value[0].price)
        assertEquals(1, sut.positions.value.size)
        assertTrue(marginCalled)
    }
}
