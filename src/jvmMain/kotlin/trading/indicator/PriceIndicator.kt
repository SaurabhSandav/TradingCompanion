package trading.indicator

import trading.Candle
import trading.CandleSeries
import trading.indicator.base.CachedIndicator
import java.math.BigDecimal

open class PriceIndicator(
    candleSeries: CandleSeries,
    description: String?,
    private val transform: (Candle) -> BigDecimal,
) : CachedIndicator<BigDecimal>(candleSeries, description) {

    override fun calculate(index: Int): BigDecimal {
        return transform(candleSeries[index])
    }
}
