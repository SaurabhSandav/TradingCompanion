package com.saurabhsandav.trading.indicator.base

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.indicator.checkIndexValid

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

    override fun get(
        from: Int,
        toInclusive: Int,
    ): List<T> {
        return (from..toInclusive).map(::get)
    }
}
