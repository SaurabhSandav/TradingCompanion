package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

open class PriceIndicator(
    candleSeries: CandleSeries,
    cacheKey: String?,
    private val transform: (Candle) -> BigDecimal,
) : CachedIndicator<BigDecimal>(candleSeries, cacheKey) {

    override fun calculate(index: Int): BigDecimal {
        return transform(candleSeries[index])
    }
}
