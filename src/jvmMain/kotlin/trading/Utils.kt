package trading

import java.math.BigDecimal

internal fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0

fun defaultIsSessionStart(candleSeries: CandleSeries, index: Int): Boolean = when (index) {
    0 -> true
    else -> {
        val candleTime = candleSeries[index].openInstant
        val lastCandleTime = candleSeries[index - 1].openInstant
        (candleTime - lastCandleTime).inWholeSeconds != candleSeries.timeframe!!.seconds
    }
}
