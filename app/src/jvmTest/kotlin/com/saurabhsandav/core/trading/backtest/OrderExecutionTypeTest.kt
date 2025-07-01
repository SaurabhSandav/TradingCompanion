package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.test.assertBDEquals
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderExecutionTypeTest {

    @Test
    fun `Limit Order Buy`() {

        val type = Limit(200.toBigDecimal())
        val side = TradeExecutionSide.Buy

        // Price crossing limit price while falling -> Execute at limit price
        assertBDEquals(200, type.tryExecute(side, 205, 195))

        // Price crossing limit price while rising -> Execute at previous price
        assertBDEquals(195, type.tryExecute(side, 195, 205))

        // Price moving while below limit price -> Execute at previous price
        // Unchanged
        assertBDEquals(190, type.tryExecute(side, 190, 190))
        // Falling
        assertBDEquals(195, type.tryExecute(side, 195, 190))
        // Rising
        assertBDEquals(190, type.tryExecute(side, 190, 195))

        // Price moving while above limit price -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 300, 300))
        // Falling
        assertNull(type.tryExecute(side, 305, 300))
        // Rising
        assertNull(type.tryExecute(side, 300, 305))

        // Price stays at limit -> Execute at limit price
        assertBDEquals(200, type.tryExecute(side, 200, 200))
    }

    @Test
    fun `Limit Order Sell`() {

        val type = Limit(200.toBigDecimal())
        val side = TradeExecutionSide.Sell

        // Price crossing limit price while rising -> Execute at limit price
        assertBDEquals(200, type.tryExecute(side, 195, 205))

        // Price crossing limit price while falling -> Execute at previous price
        assertBDEquals(205, type.tryExecute(side, 205, 195))

        // Price moving while above limit price -> Execute at previous price
        // Unchanged
        assertBDEquals(210, type.tryExecute(side, 210, 210))
        // Rising
        assertBDEquals(205, type.tryExecute(side, 205, 210))
        // Falling
        assertBDEquals(210, type.tryExecute(side, 210, 205))

        // Price moving while below limit price -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 100, 100))
        // Rising
        assertNull(type.tryExecute(side, 100, 105))
        // Falling
        assertNull(type.tryExecute(side, 105, 100))

        // Price stays at limit -> Execute at limit price
        assertBDEquals(200, type.tryExecute(side, 200, 200))
    }

    @Test
    fun `Market Order Buy`() {

        val type = Market
        val side = TradeExecutionSide.Buy

        // Always execute at new price.
        // Unchanged
        assertBDEquals(200, type.tryExecute(side, 200, 200))
        // Rising
        assertBDEquals(200, type.tryExecute(side, 199, 200))
        // Falling
        assertBDEquals(199, type.tryExecute(side, 200, 199))
    }

    @Test
    fun `Market Order Sell`() {

        val type = Market
        val side = TradeExecutionSide.Sell

        // Always execute at new price.
        // Unchanged
        assertBDEquals(200, type.tryExecute(side, 200, 200))
        // Rising
        assertBDEquals(200, type.tryExecute(side, 199, 200))
        // Falling
        assertBDEquals(199, type.tryExecute(side, 200, 199))
    }

    @Test
    fun `Stop Limit Order Buy`() {

        val type = StopLimit(
            trigger = 200.toBigDecimal(),
            price = 202.toBigDecimal(),
        )
        val side = TradeExecutionSide.Buy

        // Price crossing trigger price but not limit price while rising -> Execute at new price
        assertBDEquals(201, type.tryExecute(side, 195, 201))

        // Price crossing trigger and limit prices while rising -> Execute at limit price
        assertBDEquals(202, type.tryExecute(side, 195, 205))

        // Price crossing trigger and limit prices while falling -> Execute at limit price
        assertBDEquals(202, type.tryExecute(side, 205, 195))

        // Price crossing trigger price but not limit price while falling -> Execute at previous price
        assertBDEquals(201, type.tryExecute(side, 201, 195))

        // Price moving while above limit -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 210, 210))
        // Rising
        assertNull(type.tryExecute(side, 205, 210))
        // Falling
        assertNull(type.tryExecute(side, 210, 205))

        // Price moving while below trigger -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 100, 100))
        // Rising
        assertNull(type.tryExecute(side, 100, 105))
        // Falling
        assertNull(type.tryExecute(side, 105, 100))

        // Price stays at trigger -> Execute at trigger
        assertBDEquals(200, type.tryExecute(side, 200, 200))

        // Price stays at limit -> Execute at limit price
        assertBDEquals(202, type.tryExecute(side, 202, 202))
    }

    @Test
    fun `Stop Limit Order Sell`() {

        val type = StopLimit(
            trigger = 200.toBigDecimal(),
            price = 198.toBigDecimal(),
        )
        val side = TradeExecutionSide.Sell

        // Price crossing trigger price but not limit price while falling -> Execute at new price
        assertBDEquals(199, type.tryExecute(side, 205, 199))

        // Price crossing trigger and limit prices while falling -> Execute at limit price
        assertBDEquals(198, type.tryExecute(side, 205, 195))

        // Price crossing trigger and limit prices while rising -> Execute at limit price
        assertBDEquals(198, type.tryExecute(side, 195, 205))

        // Price crossing trigger price but not limit price while rising -> Execute at previous price
        assertBDEquals(199, type.tryExecute(side, 199, 205))

        // Price moving while below limit -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 190, 190))
        // Falling
        assertNull(type.tryExecute(side, 195, 190))
        // Rising
        assertNull(type.tryExecute(side, 190, 195))

        // Price moving while above trigger -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 300, 300))
        // Falling
        assertNull(type.tryExecute(side, 305, 300))
        // Rising
        assertNull(type.tryExecute(side, 300, 305))

        // Price stays at trigger -> Execute at trigger
        assertBDEquals(200, type.tryExecute(side, 200, 200))

        // Price stays at limit -> Execute at limit price
        assertBDEquals(198, type.tryExecute(side, 198, 198))
    }

    @Test
    fun `Stop Market Order Buy`() {

        val type = StopMarket(200.toBigDecimal())
        val side = TradeExecutionSide.Buy

        // Price crossing trigger price while rising -> Execute at new price
        assertBDEquals(201, type.tryExecute(side, 195, 201))

        // Price crossing trigger price while falling -> Execute at previous price
        assertBDEquals(205, type.tryExecute(side, 205, 195))

        // Price moving while above trigger -> Execute at previous price
        // Unchanged
        assertBDEquals(210, type.tryExecute(side, 210, 210))
        // Rising
        assertBDEquals(205, type.tryExecute(side, 205, 210))
        // Falling
        assertBDEquals(210, type.tryExecute(side, 210, 205))

        // Price moving while below trigger -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 100, 100))
        // Rising
        assertNull(type.tryExecute(side, 100, 105))
        // Falling
        assertNull(type.tryExecute(side, 105, 100))

        // Price stays at trigger -> Execute at trigger
        assertBDEquals(200, type.tryExecute(side, 200, 200))
    }

    @Test
    fun `Stop Market Order Sell`() {

        val type = StopMarket(200.toBigDecimal())
        val side = TradeExecutionSide.Sell

        // Price crossing trigger price while falling -> Execute at new price
        assertBDEquals(199, type.tryExecute(side, 205, 199))

        // Price crossing trigger price while rising -> Execute at previous price
        assertBDEquals(195, type.tryExecute(side, 195, 205))

        // Price moving while below trigger -> Execute at previous price
        // Unchanged
        assertBDEquals(190, type.tryExecute(side, 190, 190))
        // Falling
        assertBDEquals(195, type.tryExecute(side, 195, 190))
        // Rising
        assertBDEquals(190, type.tryExecute(side, 190, 195))

        // Price moving while above trigger -> Don't execute
        // Unchanged
        assertNull(type.tryExecute(side, 300, 300))
        // Falling
        assertNull(type.tryExecute(side, 305, 300))
        // Rising
        assertNull(type.tryExecute(side, 300, 305))

        // Price stays at trigger -> Execute at trigger
        assertBDEquals(200, type.tryExecute(side, 200, 200))
    }

    @Test
    fun `Trailing Stop Order Buy - Activation`() {

        fun checkActivation(
            expected: Boolean,
            prevPrice: Int,
            newPrice: Int,
        ) {

            val side = TradeExecutionSide.Buy
            val type = TrailingStop(
                callbackDecimal = "0.05".toBigDecimal(),
                activationPrice = 200.toBigDecimal(),
            )

            type.tryExecute(side, prevPrice, newPrice)

            assertEquals(expected, type.isActivated)
        }

        // Price crossing activation price while falling -> Activate
        checkActivation(true, 205, 199)

        // Price crossing activation price while rising -> Activate
        checkActivation(true, 195, 205)

        // Price moving while below activation price -> Activate
        // Unchanged
        checkActivation(true, 190, 190)
        // Falling
        checkActivation(true, 195, 190)
        // Rising
        checkActivation(true, 190, 195)

        // Price moving while above activation price -> Don't activate
        // Unchanged
        checkActivation(false, 300, 300)
        // Falling
        checkActivation(false, 305, 300)
        // Rising
        checkActivation(false, 300, 305)

        // Price stays at activation price -> Activate
        checkActivation(true, 200, 200)
    }

    @Test
    fun `Trailing Stop Order Sell - Activation`() {

        fun checkActivation(
            expected: Boolean,
            prevPrice: Int,
            newPrice: Int,
        ) {

            val side = TradeExecutionSide.Sell
            val type = TrailingStop(
                callbackDecimal = "0.05".toBigDecimal(),
                activationPrice = 200.toBigDecimal(),
            )

            assertFalse(type.isActivated)

            type.tryExecute(side, prevPrice, newPrice)

            assertEquals(expected, type.isActivated)
        }

        // Price crossing activation price while rising -> Activate
        checkActivation(true, 195, 201)

        // Price crossing activation price while falling -> Activate
        checkActivation(true, 205, 195)

        // Price moving while above activation price -> Activate
        // Unchanged
        checkActivation(true, 210, 210)
        // Rising
        checkActivation(true, 205, 210)
        // Falling
        checkActivation(true, 210, 205)

        // Price moving while below activation price -> Don't activate
        // Unchanged
        checkActivation(false, 100, 100)
        // Rising
        checkActivation(false, 100, 105)
        // Falling
        checkActivation(false, 105, 100)

        // Price stays at activation price -> Activate
        checkActivation(true, 200, 200)
    }

    @Test
    fun `Trailing Stop Order Buy - Execute`() {

        // Values from binance spot trailing stop examples

        val type = TrailingStop(
            callbackDecimal = "0.05".toBigDecimal(),
            activationPrice = 9_000.toBigDecimal(),
        )
        val side = TradeExecutionSide.Buy

        type.tryExecute(side, 10_000, 9_500)
        assertBDEquals(9_975, type.trailingStop)
        assertFalse(type.isActivated)

        type.tryExecute(side, 9_500, 9_000)
        assertBDEquals(9_450, type.trailingStop)
        assertTrue(type.isActivated)

        type.tryExecute(side, 9_000, 8_500)
        assertBDEquals(8_925, type.trailingStop)

        type.tryExecute(side, 8_500, 8_800)
        assertBDEquals(8_925, type.trailingStop)

        type.tryExecute(side, 8_800, 8_000)
        assertBDEquals(8_400, type.trailingStop)

        val executionPrice = type.tryExecute(side, 8_000, 8_500)
        assertBDEquals(8_400, type.trailingStop)
        assertBDEquals(8_500, executionPrice)
    }

    @Test
    fun `Trailing Stop Order Sell - Execute`() {

        // Values from binance spot trailing stop examples

        val type = TrailingStop(
            callbackDecimal = "0.05".toBigDecimal(),
            activationPrice = 8_500.toBigDecimal(),
        )
        val side = TradeExecutionSide.Sell

        type.tryExecute(side, 7_500, 8_000)
        assertBDEquals(7_600, type.trailingStop)
        assertFalse(type.isActivated)

        type.tryExecute(side, 8_000, 8_500)
        assertBDEquals(8_075, type.trailingStop)
        assertTrue(type.isActivated)

        type.tryExecute(side, 8_500, 16_000)
        assertBDEquals(15_200, type.trailingStop)

        type.tryExecute(side, 16_000, 15_700)
        assertBDEquals(15_200, type.trailingStop)

        type.tryExecute(side, 15_700, 18_000)
        assertBDEquals(17_100, type.trailingStop)

        val executionPrice = type.tryExecute(side, 18_000, 17_000)
        assertBDEquals(17_100, type.trailingStop)
        assertBDEquals(17_000, executionPrice)
    }

    @Test
    fun `Trailing Stop Order Sell - Execute 2`() {

        // Values from binance trailing stop examples

        val type = TrailingStop(
            callbackDecimal = "0.05".toBigDecimal(),
            activationPrice = 10_500.toBigDecimal(),
        )
        val side = TradeExecutionSide.Sell

        type.tryExecute(side, 9_500, 10_000)
        assertBDEquals(9_500, type.trailingStop)
        assertFalse(type.isActivated)

        type.tryExecute(side, 10_000, 10_500)
        assertBDEquals(9_975, type.trailingStop)
        assertTrue(type.isActivated)

        type.tryExecute(side, 10_500, 10_200)
        assertBDEquals(9_975, type.trailingStop)

        type.tryExecute(side, 10_200, 11_000)
        assertBDEquals(10_450, type.trailingStop)

        val executionPrice = type.tryExecute(side, 11_000, 10_300)
        assertBDEquals(10_450, type.trailingStop)
        assertBDEquals(10_300, executionPrice)
    }

    private fun OrderExecutionType.tryExecute(
        side: TradeExecutionSide,
        prevPrice: Int,
        newPrice: Int,
    ): BigDecimal? = tryExecute(side, prevPrice.toBigDecimal(), newPrice.toBigDecimal())
}
