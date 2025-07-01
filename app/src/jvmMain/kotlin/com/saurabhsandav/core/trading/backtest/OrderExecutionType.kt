package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.trading.record.model.TradeExecutionSide
import java.math.BigDecimal

sealed interface OrderExecutionType {

    fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal?
}

data class Limit(
    val price: BigDecimal,
) : OrderExecutionType {

    override fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal? {

        when (side) {
            // Buy Limit is executed when price crosses limit price while falling
            TradeExecutionSide.Buy -> {

                // If price was already below limit price -> Execute at previous price
                if (prevPrice <= price) return prevPrice

                // Price fell or didn't change. If price crossed limit price -> Execute at limit price
                if (prevPrice >= newPrice && newPrice <= price) return price
            }

            // Sell Limit is executed when price crosses limit price while rising
            TradeExecutionSide.Sell -> {

                // If price was already above limit price -> Execute at previous price
                if (prevPrice >= price) return prevPrice

                // Price rose or didn't change. If price crossed limit price -> Execute at limit price
                if (prevPrice <= newPrice && newPrice >= price) return price
            }
        }

        return null
    }
}

data object Market : OrderExecutionType {

    override fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal {
        // Always executes at new price
        return newPrice
    }
}

data class StopLimit(
    val trigger: BigDecimal,
    val price: BigDecimal,
) : OrderExecutionType {

    override fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal? {

        require(
            value = when (side) {
                TradeExecutionSide.Buy -> trigger <= price
                TradeExecutionSide.Sell -> trigger >= price
            },
        ) {

            val (sideStr, comparisonStr) = when (side) {
                TradeExecutionSide.Buy -> "buy" to "more"
                TradeExecutionSide.Sell -> "sell" to "less"
            }

            "For a $sideStr StopLimit order, price should be $comparisonStr than or equal to trigger"
        }

        when (side) {
            // Buy StopLimit is executed when price crosses trigger price while rising
            TradeExecutionSide.Buy -> {

                // If price moving above limit price -> Don't execute
                if (prevPrice > price && newPrice > price) return null

                // If price crossing limit while falling -> Execute at limit price
                if (prevPrice > price && price > newPrice) return price

                // If price crossing trigger price but not limit price while falling -> Execute at previous price
                if (prevPrice > trigger && trigger > newPrice) return prevPrice

                // Price crossed trigger while rising -> Execute at new price at most limit price
                if (newPrice >= trigger) return minOf(newPrice, price)
            }

            // Sell StopLimit is executed when price crosses trigger price while falling
            TradeExecutionSide.Sell -> {

                // If price moving below limit price -> Don't execute
                if (prevPrice < price && newPrice < price) return null

                // If price crossing limit while rising -> Execute at limit price
                if (prevPrice < price && price < newPrice) return price

                // If price crossing trigger price but not limit price while rising -> Execute at previous price
                if (prevPrice < trigger && trigger < newPrice) return prevPrice

                // Price crossed trigger while falling -> Execute at new price at least limit price
                if (newPrice <= trigger) return maxOf(newPrice, price)
            }
        }

        return null
    }
}

data class StopMarket(
    val trigger: BigDecimal,
) : OrderExecutionType {

    override fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal? {

        when (side) {
            // Buy StopMarket is executed when price crosses trigger price while rising
            TradeExecutionSide.Buy -> {

                // If price was already above trigger -> Execute at previous price
                if (prevPrice >= trigger) return prevPrice

                // Price rose or didn't change. If price crossed trigger -> Execute at new price
                if (prevPrice <= newPrice && newPrice >= trigger) return newPrice
            }

            // Sell StopMarket is executed when price crosses trigger price while falling
            TradeExecutionSide.Sell -> {

                // If price was already below trigger -> Execute at previous price
                if (prevPrice <= trigger) return prevPrice

                // Price fell or didn't change. If price crossed trigger -> Execute at new price
                if (prevPrice >= newPrice && newPrice <= trigger) return newPrice
            }
        }

        return null
    }
}

data class TrailingStop(
    val callbackDecimal: BigDecimal,
    val activationPrice: BigDecimal,
) : OrderExecutionType {

    internal var isActivated: Boolean = false
    var trailingStop: BigDecimal? = null
        private set

    override fun tryExecute(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ): BigDecimal? {

        isActivated = when (side) {
            // Buy TrailingStop is activated when price crosses activation price
            TradeExecutionSide.Buy -> prevPrice <= activationPrice || newPrice <= activationPrice

            // Sell TrailingStop is activated when price crosses activation price
            TradeExecutionSide.Sell -> prevPrice >= activationPrice || newPrice >= activationPrice
        }

        updateTrailingStop(side, prevPrice, newPrice)

        if (!isActivated) return null

        when (side) {
            // Buy TrailingStop is executed when price crosses trailing stop while rising
            TradeExecutionSide.Buy -> if (newPrice > trailingStop) return newPrice
            // Buy TrailingStop is executed when price crosses trailing stop while falling
            TradeExecutionSide.Sell -> if (newPrice < trailingStop) return newPrice
        }

        // Cannot execute order
        return null
    }

    private fun updateTrailingStop(
        side: TradeExecutionSide,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ) {

        val newExtremePrice = when (side) {
            TradeExecutionSide.Buy -> minOf(prevPrice, newPrice)
            TradeExecutionSide.Sell -> maxOf(prevPrice, newPrice)
        }

        // Calculate new trailing stop
        val newTrailingStop = newExtremePrice * when (side) {
            TradeExecutionSide.Buy -> BigDecimal.ONE + callbackDecimal
            TradeExecutionSide.Sell -> BigDecimal.ONE - callbackDecimal
        }

        trailingStop = when (val trailingStop = trailingStop) {
            null -> newTrailingStop
            else -> when (side) {
                TradeExecutionSide.Buy -> minOf(trailingStop, newTrailingStop)
                TradeExecutionSide.Sell -> maxOf(trailingStop, newTrailingStop)
            }
        }
    }
}
