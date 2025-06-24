package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.Indicator
import java.math.BigDecimal

class ClosePriceIndicator(
    override val candleSeries: CandleSeries,
) : Indicator<BigDecimal> {

    override val cacheKey: Indicator.CacheKey = CacheKey

    override fun get(index: Int): BigDecimal {
        return candleSeries[index].close
    }

    private object CacheKey : Indicator.CacheKey
}
