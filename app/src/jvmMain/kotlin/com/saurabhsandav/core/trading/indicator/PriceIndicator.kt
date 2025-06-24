package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.core.Candle
import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

open class PriceIndicator(
    candleSeries: CandleSeries,
    cacheKey: Indicator.CacheKey?,
    private val transform: (Candle) -> BigDecimal,
) : CachedIndicator<BigDecimal>(candleSeries, cacheKey) {

    override fun calculate(index: Int): BigDecimal {
        return transform(candleSeries[index])
    }
}
