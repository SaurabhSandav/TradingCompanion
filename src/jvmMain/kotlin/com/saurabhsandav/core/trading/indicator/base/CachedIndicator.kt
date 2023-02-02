package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.core.trading.CandleSeries

abstract class CachedIndicator<T : Any>(
    final override val candleSeries: CandleSeries,
    final override val description: String?,
) : Indicator<T> {

    private val cache = candleSeries.getIndicatorCache<T>(description)

    protected abstract fun calculate(index: Int): T

    override fun get(index: Int): T {

        checkIndexValid(index)

        if (cache[index] == null) {
            cache[index] = calculate(index)
        }

        return cache[index]!!
    }
}
