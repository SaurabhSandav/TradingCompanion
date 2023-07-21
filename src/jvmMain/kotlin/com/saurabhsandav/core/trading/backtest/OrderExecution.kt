package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trades.model.OrderType
import java.math.BigDecimal

sealed interface OrderExecution {

    fun canExecute(
        type: OrderType,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): Boolean

    data class Limit(
        val price: BigDecimal,
    ) : OrderExecution {

        override fun canExecute(
            type: OrderType,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return price.isBetween(prevPrice, newPrice)
        }
    }

    data object Market : OrderExecution {

        override fun canExecute(
            type: OrderType,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean = true
    }

    data class StopLimit(
        val trigger: BigDecimal,
        val limitPrice: BigDecimal,
    ) : OrderExecution {

        override fun canExecute(
            type: OrderType,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return trigger.isBetween(prevPrice, newPrice)
        }
    }

    data class StopMarket(
        val trigger: BigDecimal,
    ) : OrderExecution {

        override fun canExecute(
            type: OrderType,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {
            return trigger.isBetween(prevPrice, newPrice)
        }
    }

    data class TrailingStop(
        private val orderType: OrderType,
        val callbackRate: BigDecimal,
        val activationPrice: BigDecimal,
    ) : OrderExecution {

        private val callbackDecimal = callbackRate / 100.toBigDecimal()

        private var isActivated: Boolean = false
        var trailingStop: BigDecimal = activationPrice * when (orderType) {
            OrderType.Buy -> BigDecimal.ONE + callbackDecimal
            OrderType.Sell -> BigDecimal.ONE - callbackDecimal
        }

        override fun canExecute(
            type: OrderType,
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean {

            // Activate trailingStop if price crossed activation price
            if (!isActivated) {
                isActivated = activationPrice.isBetween(prevPrice, newPrice)
                // Don't activate trailing stop and execute order in the same price movement
                return false
            }

            // Return true if trailing stop hit
            if (trailingStop.isBetween(prevPrice, newPrice)) return true

            val newExtremePrice = when (orderType) {
                OrderType.Buy -> minOf(prevPrice, newPrice)
                OrderType.Sell -> maxOf(prevPrice, newPrice)
            }

            // Calculate new trailing stop
            val newTrailingStop = newExtremePrice * when (orderType) {
                OrderType.Buy -> BigDecimal.ONE + callbackDecimal
                OrderType.Sell -> BigDecimal.ONE - callbackDecimal
            }

            trailingStop = when (orderType) {
                OrderType.Buy -> minOf(trailingStop, newTrailingStop)
                OrderType.Sell -> maxOf(trailingStop, newTrailingStop)
            }

            // Cannot execute order
            return false
        }
    }
}

private fun BigDecimal.isBetween(first: BigDecimal, second: BigDecimal): Boolean {
    return (first < second && first <= this && this <= second) || (first > second && second <= this && this <= first)
}
