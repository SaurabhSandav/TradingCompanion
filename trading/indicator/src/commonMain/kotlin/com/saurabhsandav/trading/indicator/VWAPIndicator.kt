package com.saurabhsandav.trading.indicator

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class VWAPIndicator(
    candleSeries: CandleSeries,
    sessionChecker: SessionChecker,
) : CachedIndicator<BigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey(sessionChecker),
    ) {

    private val typicalPrice = TypicalPriceIndicator(candleSeries)
    private val volume = VolumeIndicator(candleSeries)
    private val tpv = ProductIndicator(typicalPrice, volume)
    private val cumulativeTPV = SessionCumulativeIndicator(tpv, sessionChecker)
    private val cumulativeVolume = SessionCumulativeIndicator(volume, sessionChecker)

    override fun calculate(index: Int): BigDecimal {
        return when {
            cumulativeVolume[index] == BigDecimal.ZERO -> BigDecimal.ZERO
            else -> cumulativeTPV[index].divide(cumulativeVolume[index], mathContext)
        }
    }

    private data class CacheKey(
        val sessionChecker: SessionChecker,
    ) : Indicator.CacheKey
}
