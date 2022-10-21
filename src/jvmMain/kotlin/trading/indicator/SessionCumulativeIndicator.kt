package trading.indicator

import trading.Candle
import trading.indicator.base.CachedIndicator
import trading.indicator.base.Indicator
import java.math.BigDecimal

class SessionCumulativeIndicator(
    private val input: Indicator<BigDecimal>,
    private val isSessionStart: (Candle) -> Boolean,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    description = null,
) {

    override fun calculate(index: Int): BigDecimal {

        val accumulated = when {
            isSessionStart(candleSeries[index]) -> BigDecimal.ZERO
            else -> get(index - 1)
        }

        return accumulated + input[index]
    }
}
