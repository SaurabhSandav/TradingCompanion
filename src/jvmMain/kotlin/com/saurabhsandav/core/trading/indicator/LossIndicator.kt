package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal

class LossIndicator(
    private val input: Indicator<BigDecimal>,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    description = "LossIndicator(${input.description})",
) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return BigDecimal.ZERO

        return when {
            input[index] < input[index - 1] -> input[index - 1] - input[index]
            else -> BigDecimal.ZERO
        }
    }
}
