package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import kotlin.math.max

class VolumeIndicator(
    candleSeries: CandleSeries,
    private val length: Int = 1,
) : CachedIndicator<KBigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey(length),
    ) {

    override fun calculate(index: Int): KBigDecimal {

        val startIndex = max(0, index - length + 1)
        var volumeSum = KBigDecimal.Zero

        for (i in startIndex..index) {
            volumeSum += candleSeries[i].volume
        }

        return volumeSum
    }

    private data class CacheKey(
        val length: Int,
    ) : Indicator.CacheKey
}
