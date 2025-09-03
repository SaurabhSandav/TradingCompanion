package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.isZero
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class RSIIndicator(
    input: Indicator<KBigDecimal>,
    length: Int = 14,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                length = length,
            )
        },
    ) {

    private val averageGain = MMAIndicator(GainIndicator(input), length)
    private val averageLoss = MMAIndicator(LossIndicator(input), length)

    override fun calculate(index: Int): KBigDecimal {

        val averageGainAtIndex = averageGain[index]
        val averageLossAtIndex = averageLoss[index]

        val hundred = 100.toKBigDecimal()

        if (averageLossAtIndex.isZero()) {
            return when {
                averageGainAtIndex.isZero() -> KBigDecimal.Zero
                else -> hundred
            }
        }

        val relativeStrength = averageGainAtIndex.div(averageLossAtIndex, mathContext)

        return hundred - (hundred.div(KBigDecimal.One + relativeStrength, mathContext))
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val length: Int,
    ) : Indicator.CacheKey
}
