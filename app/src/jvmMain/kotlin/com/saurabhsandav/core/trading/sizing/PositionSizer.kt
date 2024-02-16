package com.saurabhsandav.core.trading.sizing

import java.math.BigDecimal

fun interface PositionSizer {

    fun PositionSizerScope.size(): BigDecimal?

    companion object
}

interface PositionSizerScope {

    val balance: BigDecimal

    val entry: BigDecimal

    val stop: BigDecimal

    val leverage: BigDecimal

    val minimumQuantity: BigDecimal

    companion object {

        operator fun invoke(
            balance: BigDecimal,
            entry: BigDecimal,
            stop: BigDecimal,
            leverage: BigDecimal = BigDecimal.ONE,
            minimumQuantity: BigDecimal = BigDecimal.ZERO,
        ) = object : PositionSizerScope {

            init {
                require(entry.compareTo(stop) != 0) { "Entry and stop cannot be same" }
            }

            override val balance: BigDecimal = balance
            override val entry: BigDecimal = entry
            override val stop: BigDecimal = stop
            override val leverage: BigDecimal = leverage
            override val minimumQuantity: BigDecimal = minimumQuantity
        }
    }
}

val PositionSizerScope.stopSpread: BigDecimal
    get() = (entry - stop).abs()

fun PositionSizerScope.usableBalance(
    riskAmount: BigDecimal,
    riskToBufferMarginRatio: BigDecimal,
): BigDecimal {
    val bufferAmount = riskAmount * riskToBufferMarginRatio
    return balance - bufferAmount
}

fun PositionSizerScope.maxSize(balance: BigDecimal): BigDecimal {
    return (balance * leverage) / entry
}
