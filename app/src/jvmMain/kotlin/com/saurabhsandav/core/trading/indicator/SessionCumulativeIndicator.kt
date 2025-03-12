package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.buildIndicatorCacheKey
import java.math.BigDecimal

class SessionCumulativeIndicator(
    private val input: Indicator<BigDecimal>,
    private val sessionChecker: SessionChecker,
) : CachedIndicator<BigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                sessionChecker = sessionChecker,
            )
        },
    ) {

    override fun calculate(index: Int): BigDecimal {

        val accumulated = when {
            sessionChecker.isSessionStart(candleSeries, index) || index == 0 -> BigDecimal.ZERO
            else -> get(index - 1)
        }

        return accumulated + input[index]
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val sessionChecker: SessionChecker,
    ) : Indicator.CacheKey
}
