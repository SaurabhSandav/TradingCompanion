package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.Indicator
import java.math.BigDecimal

class PositiveMoneyFlowIndicator(
    private val price: Indicator<BigDecimal>,
    private val moneyFlow: MoneyFlowIndicator,
) : CachedIndicator<BigDecimal>(
    candleSeries = price.candleSeries,
    cacheKey = "PositiveMoneyFlowIndicator(${price.cacheKey}, ${moneyFlow.cacheKey})",
) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return BigDecimal.ZERO

        return when {
            price[index] > price[index - 1] -> moneyFlow[index]
            else -> BigDecimal.ZERO
        }
    }
}
