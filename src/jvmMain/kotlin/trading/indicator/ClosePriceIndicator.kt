package trading.indicator

import trading.CandleSeries

class ClosePriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    description = "ClosePriceIndicator",
    transform = { it.close }
)
