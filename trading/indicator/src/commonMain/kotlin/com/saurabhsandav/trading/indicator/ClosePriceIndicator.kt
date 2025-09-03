package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator

class ClosePriceIndicator(
    override val candleSeries: CandleSeries,
) : Indicator<KBigDecimal> {

    override val cacheKey: Indicator.CacheKey = CacheKey

    override fun get(index: Int): KBigDecimal {
        return candleSeries[index].close
    }

    private object CacheKey : Indicator.CacheKey
}
