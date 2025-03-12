package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.RecursiveCachedIndicator
import com.saurabhsandav.core.trading.indicator.base.buildIndicatorCacheKey
import java.math.BigDecimal

abstract class AbstractEMAIndicator(
    private val input: Indicator<BigDecimal>,
    private val multiplier: BigDecimal,
) : RecursiveCachedIndicator<BigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                multiplier = multiplier,
            )
        },
    ) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return input[0]

        val prevEMA = get(index - 1)
        val input = input[index]

        return (input - prevEMA).multiply(multiplier, mathContext) + prevEMA
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val multiplier: BigDecimal,
    ) : Indicator.CacheKey
}
