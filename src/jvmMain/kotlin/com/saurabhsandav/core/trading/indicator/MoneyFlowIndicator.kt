package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal

class MoneyFlowIndicator(
    private val price: Indicator<BigDecimal>,
    private val volume: VolumeIndicator,
) : CachedIndicator<BigDecimal>(
    candleSeries = price.candleSeries,
    description = "MoneyFlowIndicator(${price.description}, ${volume.description})",
) {

    override fun calculate(index: Int): BigDecimal {
        return price[index].multiply(volume[index], mathContext)
    }
}
