package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.isZero
import java.math.BigDecimal

class RSIIndicator(
    input: Indicator<BigDecimal>,
    length: Int = 14,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    cacheKey = "RelativeStrengthIndex(${input.cacheKey}, $length)",
) {

    private val averageGain = MMAIndicator(GainIndicator(input), length)
    private val averageLoss = MMAIndicator(LossIndicator(input), length)

    override fun calculate(index: Int): BigDecimal {

        val averageGainAtIndex = averageGain[index]
        val averageLossAtIndex = averageLoss[index]

        val hundred = 100.toBigDecimal()

        if (averageLossAtIndex.isZero()) {
            return when {
                averageGainAtIndex.isZero() -> BigDecimal.ZERO
                else -> hundred
            }
        }

        val relativeStrength = averageGainAtIndex.divide(averageLossAtIndex, mathContext)

        return hundred - (hundred.divide(BigDecimal.ONE + relativeStrength, mathContext))
    }
}
