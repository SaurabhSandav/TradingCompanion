package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.indicator.base.buildIndicatorCacheKey
import java.math.BigDecimal

class NegativeMoneyFlowIndicator(
    private val price: Indicator<BigDecimal>,
    private val moneyFlow: MoneyFlowIndicator,
) : CachedIndicator<BigDecimal>(
    candleSeries = price.candleSeries,
    cacheKey = buildIndicatorCacheKey {
        CacheKey(
            price = price.bindCacheKey(),
            moneyFlow = moneyFlow.bindCacheKey(),
        )
    },
) {

    override fun calculate(index: Int): BigDecimal {

        if (index == 0) return BigDecimal.ZERO

        return when {
            price[index] < price[index - 1] -> moneyFlow[index]
            else -> BigDecimal.ZERO
        }
    }

    private data class CacheKey(
        val price: Indicator.CacheKey,
        val moneyFlow: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
