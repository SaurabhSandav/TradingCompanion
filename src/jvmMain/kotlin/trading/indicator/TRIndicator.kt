package trading.indicator

import trading.CandleSeries
import trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class TRIndicator(
    candleSeries: CandleSeries,
) : CachedIndicator<BigDecimal>(
    candleSeries = candleSeries,
    description = "TrueRange",
) {

    override fun calculate(index: Int): BigDecimal {

        val ts = candleSeries.list[index].high - candleSeries.list[index].low

        val ys = when (index) {
            0 -> BigDecimal.ZERO
            else -> candleSeries.list[index].high - candleSeries.list[index - 1].close
        }

        val yst = when (index) {
            0 -> BigDecimal.ZERO
            else -> candleSeries.list[index - 1].close - candleSeries.list[index].low
        }

        return listOf(ts.abs(), ys.abs(), yst.abs()).maxOrNull()!!
    }
}
