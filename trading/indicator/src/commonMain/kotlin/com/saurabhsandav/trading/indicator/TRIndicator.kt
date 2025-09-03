package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class TRIndicator(
    candleSeries: CandleSeries,
) : CachedIndicator<KBigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey,
    ) {

    override fun calculate(index: Int): KBigDecimal {

        val ts = candleSeries[index].high - candleSeries[index].low

        val ys = when (index) {
            0 -> KBigDecimal.Zero
            else -> candleSeries[index].high - candleSeries[index - 1].close
        }

        val yst = when (index) {
            0 -> KBigDecimal.Zero
            else -> candleSeries[index - 1].close - candleSeries[index].low
        }

        return listOf(ts.abs(), ys.abs(), yst.abs()).maxOrNull()!!
    }

    private object CacheKey : Indicator.CacheKey
}
