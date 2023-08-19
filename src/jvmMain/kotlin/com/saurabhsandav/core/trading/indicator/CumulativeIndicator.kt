package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal
import kotlin.math.max

class CumulativeIndicator(
    private val input: Indicator<BigDecimal>,
    private val length: Int,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    cacheKey = "CumulativeIndicator(${input.cacheKey}, $length)",
) {

    override fun calculate(index: Int): BigDecimal {

        val startIndex = max(0, index - length + 1)
        var sum = BigDecimal.ZERO

        for (i in startIndex..index) {
            sum += input[i]
        }

        return sum
    }
}
