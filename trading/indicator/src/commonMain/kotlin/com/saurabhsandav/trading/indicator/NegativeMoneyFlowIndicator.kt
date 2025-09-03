package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class NegativeMoneyFlowIndicator(
    private val price: Indicator<KBigDecimal>,
    private val moneyFlow: MoneyFlowIndicator,
) : CachedIndicator<KBigDecimal>(
        candleSeries = price.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                price = price.bindCacheKey(),
                moneyFlow = moneyFlow.bindCacheKey(),
            )
        },
    ) {

    override fun calculate(index: Int): KBigDecimal {

        if (index == 0) return KBigDecimal.Zero

        return when {
            price[index] < price[index - 1] -> moneyFlow[index]
            else -> KBigDecimal.Zero
        }
    }

    private data class CacheKey(
        val price: Indicator.CacheKey,
        val moneyFlow: Indicator.CacheKey,
    ) : Indicator.CacheKey
}
