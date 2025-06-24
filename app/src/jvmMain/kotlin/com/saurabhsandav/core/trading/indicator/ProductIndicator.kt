package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.core.Indicator
import com.saurabhsandav.core.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class ProductIndicator(
    private val input1: Indicator<BigDecimal>,
    private val input2: Indicator<BigDecimal>,
) : CachedIndicator<BigDecimal>(
        candleSeries = input1.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input1 = input1.bindCacheKey(),
                input2 = input2.bindCacheKey(),
            )
        },
    ) {

    override fun calculate(index: Int): BigDecimal {
        return input1[index].multiply(input2[index], mathContext)
    }

    private data class CacheKey(
        val input1: Indicator.CacheKey,
        val input2: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
