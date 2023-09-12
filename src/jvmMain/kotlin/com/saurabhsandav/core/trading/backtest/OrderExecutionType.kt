package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trades.model.OrderSide
import java.math.BigDecimal

sealed interface OrderExecutionType {

    fun canExecute(
        side: OrderSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): Boolean

    data class Limit(
        val price: BigDecimal,
    ) : OrderExecutionType {

        override fun canExecute(
            side: OrderSide,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return price.isBetween(prevPrice, newPrice)
        }
    }

    data object Market : OrderExecutionType {

        override fun canExecute(
            side: OrderSide,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean = true
    }

    data class StopLimit(
        val trigger: BigDecimal,
        val limitPrice: BigDecimal,
    ) : OrderExecutionType {

        override fun canExecute(
            side: OrderSide,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return trigger.isBetween(prevPrice, newPrice)
        }
    }

    data class StopMarket(
        val trigger: BigDecimal,
    ) : OrderExecutionType {

        override fun canExecute(
            side: OrderSide,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return trigger.isBetween(prevPrice, newPrice)
        }
    }

    data class TrailingStop(
        val callbackRate: BigDecimal,
        val activationPrice: BigDecimal,
    ) : OrderExecutionType {

        private val callbackDecimal = callbackRate / 100.toBigDecimal()

        private var isActivated: Boolean = false
        var trailingStop: BigDecimal = activationPrice

        override fun canExecute(
            side: OrderSide,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {

            // Activate trailingStop if price crossed activation price
            if (!isActivated) {

                // If activated set initial trailing stop
                if (activationPrice.isBetween(prevPrice, newPrice)) {

                    isActivated = true

                    trailingStop = activationPrice * when (side) {
                        OrderSide.Buy -> BigDecimal.ONE + callbackDecimal
                        OrderSide.Sell -> BigDecimal.ONE - callbackDecimal
                    }
                }

                // Don't activate trailing stop and execute order in the same price movement
                return false
            }

            // Return true if trailing stop hit
            if (trailingStop.isBetween(prevPrice, newPrice)) return true

            val newExtremePrice = when (side) {
                OrderSide.Buy -> minOf(prevPrice, newPrice)
                OrderSide.Sell -> maxOf(prevPrice, newPrice)
            }

            // Calculate new trailing stop
            val newTrailingStop = newExtremePrice * when (side) {
                OrderSide.Buy -> BigDecimal.ONE + callbackDecimal
                OrderSide.Sell -> BigDecimal.ONE - callbackDecimal
            }

            trailingStop = when (side) {
                OrderSide.Buy -> minOf(trailingStop, newTrailingStop)
                OrderSide.Sell -> maxOf(trailingStop, newTrailingStop)
            }

            // Cannot execute order
            return false
        }
    }
}

private fun BigDecimal.isBetween(first: BigDecimal, second: BigDecimal): Boolean {
    return (first < second && first <= this && this <= second) || (first > second && second <= this && this <= first)
}
