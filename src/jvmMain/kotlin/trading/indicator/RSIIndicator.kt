package trading.indicator

import trading.indicator.base.CachedIndicator
import trading.indicator.base.Indicator
import trading.isZero
import java.math.BigDecimal

class RSIIndicator(
    input: Indicator<BigDecimal>,
    length: Int = 14,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    description = "RelativeStrengthIndex(${input.description}, $length)",
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
