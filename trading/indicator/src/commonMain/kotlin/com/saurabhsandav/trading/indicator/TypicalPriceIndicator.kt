package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator

class TypicalPriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
        candleSeries = candleSeries,
        cacheKey = CacheKey,
        transform = { candle ->
            (candle.high + candle.low + candle.close).div(3.toKBigDecimal(), candleSeries.indicatorMathContext)
        },
    ) {

    private object CacheKey : Indicator.CacheKey
}
