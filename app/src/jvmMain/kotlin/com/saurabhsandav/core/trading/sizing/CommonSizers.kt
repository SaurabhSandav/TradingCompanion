package com.saurabhsandav.core.trading.sizing

import java.math.BigDecimal

fun PositionSizer.Companion.constant(
    size: BigDecimal,
    riskToBufferMarginRatio: BigDecimal = 2.toBigDecimal(),
): PositionSizer = PositionSizer {

    if (balance <= BigDecimal.ZERO) return@PositionSizer null

    val riskAmount = stopSpread * size

    val maxSize = maxSize(usableBalance(riskAmount, riskToBufferMarginRatio))

    val finalSize = minOf(maxSize, size)

    finalSize.takeIf { it > minimumQuantity }
}

fun PositionSizer.Companion.fixedRisk(
    riskAmount: BigDecimal,
    riskToBufferMarginRatio: BigDecimal = 2.toBigDecimal(),
): PositionSizer = PositionSizer {

    if (balance <= BigDecimal.ZERO) return@PositionSizer null

    val calcSize = riskAmount / stopSpread

    val maxSize = maxSize(usableBalance(riskAmount, riskToBufferMarginRatio))

    val finalSize = minOf(maxSize, calcSize)

    finalSize.takeIf { it > minimumQuantity }
}

fun PositionSizer.Companion.accountRisk(
    riskDecimal: BigDecimal,
    riskToBufferMarginRatio: BigDecimal = 2.toBigDecimal(),
): PositionSizer = PositionSizer {

    if (balance <= BigDecimal.ZERO) return@PositionSizer null

    val riskAmount = balance * riskDecimal
    val calcSize = riskAmount / stopSpread

    val maxSize = maxSize(usableBalance(riskAmount, riskToBufferMarginRatio))

    val finalSize = minOf(maxSize, calcSize)

    finalSize.takeIf { it > minimumQuantity }
}
