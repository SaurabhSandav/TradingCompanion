package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import java.math.BigDecimal

class ATRIndicator(
    override val candleSeries: CandleSeries,
    length: Int = 14,
) : Indicator<BigDecimal> {

    private val atr = MMAIndicator(TRIndicator(candleSeries), length)

    override val cacheKey: Indicator.CacheKey? = atr.cacheKey

    override fun get(index: Int): BigDecimal {
        return atr[index]
    }
}
