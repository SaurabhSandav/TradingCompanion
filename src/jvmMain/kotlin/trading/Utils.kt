package trading

import java.math.BigDecimal

internal fun BigDecimal.isZero(): Boolean = compareTo(BigDecimal.ZERO) == 0

fun dailySessionStart(candleSeries: CandleSeries, index: Int): Boolean = when {
    index == 0 -> true
    (candleSeries.timeframe ?: error("Timeframe is necessary to detect session start")) >= Timeframe.D1 -> false
    else -> {
        val candleTime = candleSeries[index].openInstant
        val lastCandleTime = candleSeries[index - 1].openInstant
        (candleTime - lastCandleTime).inWholeSeconds != candleSeries.timeframe.seconds
    }
}
