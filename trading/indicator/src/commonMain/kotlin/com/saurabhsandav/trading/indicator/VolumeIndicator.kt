package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import java.math.BigDecimal
import kotlin.math.max

class VolumeIndicator(
    candleSeries: CandleSeries,
    private val length: Int = 1,
) : CachedIndicator<BigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey(length),
    ) {

    override fun calculate(index: Int): BigDecimal {

        val startIndex = max(0, index - length + 1)
        var volumeSum = BigDecimal.ZERO

        for (i in startIndex..index) {
            volumeSum += candleSeries[i].volume
        }

        return volumeSum
    }

    private data class CacheKey(
        val length: Int,
    ) : Indicator.CacheKey
}
