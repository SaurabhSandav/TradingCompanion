package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.RecursiveCachedIndicator

abstract class AbstractEMAIndicator(
    private val input: Indicator<KBigDecimal>,
    private val multiplier: KBigDecimal,
) : RecursiveCachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                multiplier = multiplier,
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        if (index == 0) return input[0]

        val prevEMA = get(index - 1)
        val input = input[index]

        return (input - prevEMA).times(multiplier, mathContext) + prevEMA
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val multiplier: KBigDecimal,
    ) : Indicator.CacheKey
}
