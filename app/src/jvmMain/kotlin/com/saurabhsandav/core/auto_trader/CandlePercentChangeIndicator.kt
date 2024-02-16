package com.saurabhsandav.core.auto_trader

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

/*class SessionPercentChangeIndicator(
    candleSeries: CandleSeries,
): CachedIndicator<BigDecimal>(
    candleSeries = candleSeries,
    cacheKey = Key,
) {

    override fun calculate(index: Int): BigDecimal {

        val open = candleSeries[index].open
        val close = candleSeries[index].close

        return ((close - open).divide(open, mathContext)) * 100.toBigDecimal()
    }

    private object Key: Indicator.CacheKey
}*/

class CandlePercentChangeIndicator(
    candleSeries: CandleSeries,
    private val length: Int = 1,
) : CachedIndicator<BigDecimal>(
        candleSeries = candleSeries,
        cacheKey = Key(length),
    ) {

    init {
        require(length >= 1) { "CandlePercentChangeIndicator: length should be 1 or greater" }
    }

    private val hundred = 100.toBigDecimal()

    override fun calculate(index: Int): BigDecimal {

        val startIndex = (index - (length - 1)).coerceAtLeast(0)

        val open = candleSeries[startIndex].open
        val close = candleSeries[index].close

        return ((close - open).divide(open, mathContext)) * hundred
    }

    private data class Key(
        val length: Int,
    ) : Indicator.CacheKey
}

class TravelIndicator(
    candleSeries: CandleSeries,
    private val length: Int = 1,
) : CachedIndicator<BigDecimal>(
        candleSeries = candleSeries,
        cacheKey = Key(length),
    ) {

    init {
        require(length >= 1) { "TravelIndicator: length should be 1 or greater" }
    }

    override fun calculate(index: Int): BigDecimal {

        val startIndex = (index - (length - 1)).coerceAtLeast(0)

        val open = candleSeries[startIndex].open
        val close = candleSeries[index].close

        return close - open
    }

    private data class Key(
        val length: Int,
    ) : Indicator.CacheKey
}
