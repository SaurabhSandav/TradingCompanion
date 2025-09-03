package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import kotlin.math.max

class CumulativeIndicator(
    private val input: Indicator<KBigDecimal>,
    private val length: Int,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                length = length,
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        val startIndex = max(0, index - length + 1)
        var sum = KBigDecimal.Zero

        for (i in startIndex..index) {
            sum += input[i]
        }

        return sum
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val length: Int,
    ) : Indicator.CacheKey
}
