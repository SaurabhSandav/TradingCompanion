package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import java.math.BigDecimal

class MoneyFlowIndicator(
    private val price: Indicator<BigDecimal>,
    private val volume: VolumeIndicator,
) : CachedIndicator<BigDecimal>(
    candleSeries = price.candleSeries,
    cacheKey = "MoneyFlowIndicator(${price.cacheKey}, ${volume.cacheKey})",
) {

    override fun calculate(index: Int): BigDecimal {
        return price[index].multiply(volume[index], mathContext)
    }
}
