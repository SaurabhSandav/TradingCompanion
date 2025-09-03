package com.saurabhsandav.trading.indicator

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.isZero
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.Indicator
import com.saurabhsandav.trading.core.buildIndicatorCacheKey
import com.saurabhsandav.trading.indicator.base.CachedIndicator

class MFIIndicator(
    input: Indicator<KBigDecimal>,
    length: Int = 14,
) : CachedIndicator<KBigDecimal>(
        candleSeries = input.candleSeries,
        cacheKey = buildIndicatorCacheKey {
            CacheKey(
                input = input.bindCacheKey(),
                length = length,
            )
        },
    ) {

    constructor(
        candleSeries: CandleSeries,
        length: Int = 14,
    ) : this(
        input = TypicalPriceIndicator(candleSeries),
        length = length,
    )

    private val moneyFlow = MoneyFlowIndicator(input, VolumeIndicator(candleSeries))
    private val positiveMoneyFlow = CumulativeIndicator(PositiveMoneyFlowIndicator(input, moneyFlow), length)
    private val negativeMoneyFlow = CumulativeIndicator(NegativeMoneyFlowIndicator(input, moneyFlow), length)

    override fun calculate(index: Int): KBigDecimal {

        val positiveMoneyFlow = positiveMoneyFlow[index]
        val negativeMoneyFlow = negativeMoneyFlow[index]

        val hundred = 100.toKBigDecimal()

        if (negativeMoneyFlow.isZero()) {
            return when {
                positiveMoneyFlow.isZero() -> KBigDecimal.Zero
                else -> hundred
            }
        }

        val moneyRatio = positiveMoneyFlow.div(negativeMoneyFlow, mathContext)

        return hundred - (hundred.div(KBigDecimal.One + moneyRatio, mathContext))
    }

    private data class CacheKey(
        val input: Indicator.CacheKey,
        val length: Int,
    ) : Indicator.CacheKey
}
