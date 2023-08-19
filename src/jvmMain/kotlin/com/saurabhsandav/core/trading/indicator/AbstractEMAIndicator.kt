package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.RecursiveCachedIndicator
import java.math.BigDecimal

abstract class AbstractEMAIndicator(
    private val input: Indicator<BigDecimal>,
    private val multiplier: BigDecimal,
) : RecursiveCachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    cacheKey = "AbstractEMAIndicator(${input.cacheKey}, $multiplier)",
) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return input[0]

        val prevEMA = get(index - 1)
        val input = input[index]

        return (input - prevEMA).multiply(multiplier, mathContext) + prevEMA
    }
}
