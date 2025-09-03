package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import kotlin.math.max
import kotlin.math.min

class SMAIndicator(
    private val input: Indicator<KBigDecimal>,
    val length: Int,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                length = length,
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        var sum = KBigDecimal.Zero

        val from = max(0, (index - length + 1))

        for (i in from..index) {
            sum += input[i]
        }

        val adjLength = min(length, index + 1).toKBigDecimal()

        return sum.div(adjLength, input.mathContext)
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val length: Int,
    ) : Indicator.CacheKey
}
