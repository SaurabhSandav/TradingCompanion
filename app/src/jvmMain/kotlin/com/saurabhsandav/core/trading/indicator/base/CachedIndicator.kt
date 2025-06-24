package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.Indicator

abstract class CachedIndicator<T : Any>(
    final override val candleSeries: CandleSeries,
    final override val cacheKey: Indicator.CacheKey?,
) : Indicator<T> {

    private val cache = candleSeries.getIndicatorCache<T>(cacheKey)

    protected abstract fun calculate(index: Int): T

    override fun get(index: Int): T {

        checkIndexValid(index)

        if (cache[index] == null) {
            cache[index] = calculate(index)
        }

        return cache[index]!!
    }
}
