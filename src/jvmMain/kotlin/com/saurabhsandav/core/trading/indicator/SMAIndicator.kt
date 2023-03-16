package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

class SMAIndicator(
    private val input: Indicator<BigDecimal>,
    val length: Int,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    description = "SMA ($length)",
) {

    override fun calculate(index: Int): BigDecimal {

        var sum = BigDecimal.ZERO

        val from = max(0, (index - length + 1))

        for (i in from..index) {
            sum += input[i]
        }

        val adjLength = min(length, index + 1).toBigDecimal()

        return sum.divide(adjLength, input.mathContext)
    }
}
