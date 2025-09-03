package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator

class ATRIndicator(
    override val candleSeries: CandleSeries,
    length: Int = 14,
) : Indicator<KBigDecimal> {

    private val atr = MMAIndicator(TRIndicator(candleSeries), length)

    override val cacheKey: Indicator.CacheKey? = atr.cacheKey

    override fun get(index: Int): KBigDecimal {
        return atr[index]
    }
}
