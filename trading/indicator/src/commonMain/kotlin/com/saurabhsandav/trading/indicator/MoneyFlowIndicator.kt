package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class MoneyFlowIndicator(
    private val price: Indicator<KBigDecimal>,
    private val volume: VolumeIndicator,
) : CachedIndicator<KBigDecimal>(
        candleSeries = price.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                price = price.bindCacheKey(),
                volume = volume.bindCacheKey(),
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {
        return price[index].times(volume[index], mathContext)
    }

    private data class CacheKey(
        val price: Indicator.CacheKey,
        val volume: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
