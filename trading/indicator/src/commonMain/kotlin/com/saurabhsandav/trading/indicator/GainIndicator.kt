package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class GainIndicator(
    private val input: Indicator<KBigDecimal>,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(input = input.bindCacheKey())
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        if (index == 0) return KBigDecimal.Zero

        return when {
            input[index] > input[index - 1] -> input[index] - input[index - 1]
            else -> KBigDecimal.Zero
        }
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
