package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.isZero
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class VWAPIndicator(
    candleSeries: CandleSeries,
    sessionChecker: SessionChecker,
) : CachedIndicator<KBigDecimal>(
        candleSeries = candleSeries,
        cacheKey = CacheKey(sessionChecker),
    ) {

    private val typicalPrice = TypicalPriceIndicator(candleSeries)
    private val volume = VolumeIndicator(candleSeries)
    private val tpv = ProductIndicator(typicalPrice, volume)
    private val cumulativeTPV = SessionCumulativeIndicator(tpv, sessionChecker)
    private val cumulativeVolume = SessionCumulativeIndicator(volume, sessionChecker)

    override fun calculate(index: Int): KBigDecimal {
        return when {
            cumulativeVolume[index].isZero() -> KBigDecimal.Zero
            else -> cumulativeTPV[index].div(cumulativeVolume[index], mathContext)
        }
    }

    private data class CacheKey(
        val sessionChecker: SessionChecker,
    ) : Indicator.CacheKey
}
