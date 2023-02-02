package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries

class ClosePriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    description = "ClosePriceIndicator",
    transform = { it.close }
)
