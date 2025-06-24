package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class TRIndicator(
    candleSeries: CandleSeries,
) : CachedIndicator<BigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey,
    ) {

    override fun calculate(index: Int): BigDecimal {

        val ts = candleSeries[index].high - candleSeries[index].low

        val ys = when (index) {
            0 -> BigDecimal.ZERO
            else -> candleSeries[index].high - candleSeries[index - 1].close
        }

        val yst = when (index) {
            0 -> BigDecimal.ZERO
            else -> candleSeries[index - 1].close - candleSeries[index].low
        }

        return listOf(ts.abs(), ys.abs(), yst.abs()).maxOrNull()!!
    }

    private object CacheKey : Indicator.CacheKey
}
