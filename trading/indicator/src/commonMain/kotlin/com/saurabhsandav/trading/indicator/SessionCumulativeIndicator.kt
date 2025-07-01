package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator
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
