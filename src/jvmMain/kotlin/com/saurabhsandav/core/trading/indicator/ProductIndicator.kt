package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal

class ProductIndicator(
    private val input1: Indicator<BigDecimal>,
    private val input2: Indicator<BigDecimal>,
) : Indicator<BigDecimal> {

    override val candleSeries: CandleSeries
        get() = input1.candleSeries

    override val description: String = "ProductIndicator(${input1.description}, ${input2.description})"

    override fun get(index: Int): BigDecimal {
        return input1[index].multiply(input2[index], mathContext)
    }
}
