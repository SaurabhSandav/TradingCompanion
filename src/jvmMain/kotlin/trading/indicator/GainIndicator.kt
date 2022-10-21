package trading.indicator

import trading.indicator.base.CachedIndicator
import trading.indicator.base.Indicator
import java.math.BigDecimal

class GainIndicator(
    private val input: Indicator<BigDecimal>,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    description = "GainIndicator(${input.description})",
) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return BigDecimal.ZERO

        return when {
            input[index] > input[index - 1] -> input[index] - input[index - 1]
            else -> BigDecimal.ZERO
        }
    }
}
