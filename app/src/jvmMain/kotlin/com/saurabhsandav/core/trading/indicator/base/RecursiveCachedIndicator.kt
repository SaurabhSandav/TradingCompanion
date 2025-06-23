package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator

abstract class RecursiveCachedIndicator<T : Any>(
    candleSeries: CandleSeries,
    cacheKey: Indicator.CacheKey?,
) : CachedIndicator<T>(candleSeries, cacheKey) {

    override fun get(index: Int): T {

        if (index <= candleSeries.lastIndex) {

            if (index > RecursionThreshold) {

                for (toCacheIndex in 0..<index) {
                    super.get(toCacheIndex)
                }
            }
        }

        return super.get(index)
    }
}

private const val RecursionThreshold = 100
