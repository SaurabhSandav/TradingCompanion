package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class SessionCumulativeIndicator(
    private val input: Indicator<KBigDecimal>,
    private val sessionChecker: SessionChecker,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                sessionChecker = sessionChecker,
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        val accumulated = when {
            sessionChecker.isSessionStart(candleSeries, index) || index == 0 -> KBigDecimal.Zero
            else -> get(index - 1)
        }

        return accumulated + input[index]
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val sessionChecker: SessionChecker,
    ) : Indicator.CacheKey
}
