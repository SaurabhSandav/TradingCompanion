package com.saurabhsandav.core.trading

import java.math.MathContext

interface Indicator<T : Any> {

    val candleSeries: CandleSeries

    val mathContext: MathContext
        get() = candleSeries.indicatorMathContext

    val cacheKey: String?

    operator fun get(index: Int): T
}
