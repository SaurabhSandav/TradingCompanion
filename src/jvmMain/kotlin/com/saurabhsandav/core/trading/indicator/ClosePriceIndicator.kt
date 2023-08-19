package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries

class ClosePriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    cacheKey = "ClosePriceIndicator",
    transform = { it.close }
)
