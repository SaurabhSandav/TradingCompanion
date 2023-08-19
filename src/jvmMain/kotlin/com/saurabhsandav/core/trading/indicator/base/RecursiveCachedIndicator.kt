package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator

abstract class RecursiveCachedIndicator<T : Any>(
    candleSeries: CandleSeries,
    cacheKey: Indicator.CacheKey?,
) : CachedIndicator<T>(candleSeries, cacheKey) {

    override fun get(index: Int): T {

        if (index <= candleSeries.lastIndex) {

            if (index > RECURSION_THRESHOLD) {

                for (toCacheIndex in 0..<index) {
                    super.get(toCacheIndex)
                }
            }
        }

        return super.get(index)
    }
}

private const val RECURSION_THRESHOLD = 100
