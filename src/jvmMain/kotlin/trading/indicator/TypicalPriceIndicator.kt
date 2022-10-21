package trading.indicator

import trading.CandleSeries

class TypicalPriceIndicator(
    candleSeries: CandleSeries,
) : PriceIndicator(
    candleSeries = candleSeries,
    description = "TypicalPriceIndicator",
    transform = { candle ->
        (candle.high + candle.low + candle.close).divide(3.toBigDecimal(), candleSeries.indicatorMathContext)
    }
)
