package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class ProductIndicator(
    private val input1: Indicator<KBigDecimal>,
    private val input2: Indicator<KBigDecimal>,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input1.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input1 = input1.bindCacheKey(),
                input2 = input2.bindCacheKey(),
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {
        return input1[index].times(input2[index], mathContext)
    }

    private data class CacheKey(
        val input1: Indicator.CacheKey,
        val input2: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
