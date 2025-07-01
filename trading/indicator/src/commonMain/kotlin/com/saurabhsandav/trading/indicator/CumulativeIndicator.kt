package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import java.math.BigDecimal
import kotlin.math.max

class CumulativeIndicator(
    private val input: Indicator<BigDecimal>,
    private val length: Int,
) : CachedIndicator<BigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                length = length,
            )
        },
    ) {

    override fun calculate(index: Int): BigDecimal {

        val startIndex = max(0, index - length + 1)
        var sum = BigDecimal.ZERO

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
