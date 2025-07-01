package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class LossIndicator(
    private val input: Indicator<BigDecimal>,
) : CachedIndicator<BigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(input = input.bindCacheKey())
        },
    ) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return BigDecimal.ZERO

        return when {
            input[index] < input[index - 1] -> input[index - 1] - input[index]
            else -> BigDecimal.ZERO
        }
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
