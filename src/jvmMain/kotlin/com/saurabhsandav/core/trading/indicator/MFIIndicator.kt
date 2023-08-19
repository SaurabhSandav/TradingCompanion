package com.saurabhsandav.core.trading.indicator

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Indicator
import com.saurabhsandav.core.trading.indicator.base.CachedIndicator
import com.saurabhsandav.core.trading.isZero
import java.math.BigDecimal

class MFIIndicator(
    input: Indicator<BigDecimal>,
    length: Int = 14,
) : CachedIndicator<BigDecimal>(
    candleSeries = input.candleSeries,
    cacheKey = "MFIIndicator(${input.cacheKey}, $length)",
) {

    constructor(
        candleSeries: CandleSeries,
        length: Int = 14,
    ) : this(
        input = TypicalPriceIndicator(candleSeries),
        length = length
    )

    private val moneyFlow = MoneyFlowIndicator(input, VolumeIndicator(candleSeries))
    private val positiveMoneyFlow = CumulativeIndicator(PositiveMoneyFlowIndicator(input, moneyFlow), length)
    private val negativeMoneyFlow = CumulativeIndicator(NegativeMoneyFlowIndicator(input, moneyFlow), length)

    override fun calculate(index: Int): BigDecimal {

        val positiveMoneyFlow = positiveMoneyFlow[index]
        val negativeMoneyFlow = negativeMoneyFlow[index]

        val hundred = 100.toBigDecimal()

        if (negativeMoneyFlow.isZero()) {
            return when {
                positiveMoneyFlow.isZero() -> BigDecimal.ZERO
                else -> hundred
            }
        }

        val moneyRatio = positiveMoneyFlow.divide(negativeMoneyFlow, mathContext)

        return hundred - (hundred.divide(BigDecimal.ONE + moneyRatio, mathContext))
    }
}
