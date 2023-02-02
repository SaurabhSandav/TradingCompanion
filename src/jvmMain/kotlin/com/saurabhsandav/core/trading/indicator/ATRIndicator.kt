package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal

class ATRIndicator(
    override val candleSeries: CandleSeries,
    length: Int = 14,
) : Indicator<BigDecimal> {

    private val atr = MMAIndicator(TRIndicator(candleSeries), length)

    override val description: String = "ATRIndicator($length)"

    override fun get(index: Int): BigDecimal {
        return atr[index]
    }
}
