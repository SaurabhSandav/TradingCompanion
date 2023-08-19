package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries

class TypicalPriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    cacheKey = "TypicalPriceIndicator",
    transform = { candle ->
        (candle.high + candle.low + candle.close).divide(3.toBigDecimal(), candleSeries.indicatorMathContext)
    }
)
