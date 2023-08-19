package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator
import java.math.BigDecimal

class ProductIndicator(
    private val input1: Indicator<BigDecimal>,
    private val input2: Indicator<BigDecimal>,
) : Indicator<BigDecimal> {

    override val candleSeries: CandleSeries
        get() = input1.candleSeries

    override val cacheKey: String = "ProductIndicator(${input1.cacheKey}, ${input2.cacheKey})"

    override fun get(index: Int): BigDecimal {
        return input1[index].multiply(input2[index], mathContext)
    }
}
