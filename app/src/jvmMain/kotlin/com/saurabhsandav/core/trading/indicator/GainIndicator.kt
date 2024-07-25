package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.buildIndicatorCacheKey
import java.math.BigDecimal

class GainIndicator(
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
            input[index] > input[index - 1] -> input[index] - input[index - 1]
            else -> BigDecimal.ZERO
        }
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
