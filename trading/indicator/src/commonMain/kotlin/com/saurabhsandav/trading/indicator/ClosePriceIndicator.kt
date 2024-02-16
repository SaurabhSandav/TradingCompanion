package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import java.math.BigDecimal

class ClosePriceIndicator(
    override val candleSeries: CandleSeries,
) : Indicator<BigDecimal> {

    override val cacheKey: Indicator.CacheKey = CacheKey

    override fun get(index: Int): BigDecimal {
        return candleSeries[index].close
    }

    override fun get(
        from: Int,
        toInclusive: Int,
    ): List<BigDecimal> {
        return candleSeries.subList(from, toInclusive + 1).map { it.close }
    }

    private object CacheKey : Indicator.CacheKey
}
