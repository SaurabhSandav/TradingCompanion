package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator

class TypicalPriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    cacheKey = CacheKey,
    transform = { candle ->
        (candle.high + candle.low + candle.close).divide(3.toBigDecimal(), candleSeries.indicatorMathContext)
    }
) {

    private object CacheKey : Indicator.CacheKey
}
