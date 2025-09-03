package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.indicator.base.CachedIndicator

open class PriceIndicator(
    candleSeries: CandleSeries,
    cacheKey: Indicator.CacheKey?,
    private val transform: (Candle) -> KBigDecimal,
) : CachedIndicator<KBigDecimal>(candleSeries, cacheKey) {

    override fun calculate(index: Int): KBigDecimal {
        return transform(candleSeries[index])
    }
}
