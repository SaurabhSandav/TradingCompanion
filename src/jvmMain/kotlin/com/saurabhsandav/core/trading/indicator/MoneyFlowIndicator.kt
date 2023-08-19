package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.buildIndicatorCacheKey
import java.math.BigDecimal

class MoneyFlowIndicator(
    private val price: Indicator<BigDecimal>,
    private val volume: VolumeIndicator,
) : CachedIndicator<BigDecimal>(
    candleSeries = price.candleSeries,
    cacheKey = buildIndicatorCacheKey {
        CacheKey(
            price = price.bindCacheKey(),
            volume = volume.bindCacheKey(),
        )
    },
) {

    override fun calculate(index: Int): BigDecimal {
        return price[index].multiply(volume[index], mathContext)
    }

    private data class CacheKey(
        val price: Indicator.CacheKey,
        val volume: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
