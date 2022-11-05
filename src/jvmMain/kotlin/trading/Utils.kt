package trading

import java.math.BigDecimal

internal fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0

fun defaultIsSessionStart(candleSeries: CandleSeries, index: Int): Boolean = when (index) {
    0 -> true
    else -> {
        val candleTime = candleSeries.list[index].openInstant
        val lastCandleTime = candleSeries.list[index - 1].openInstant
        (candleTime - lastCandleTime).inWholeSeconds != candleSeries.timeframe!!.seconds
    }
}
