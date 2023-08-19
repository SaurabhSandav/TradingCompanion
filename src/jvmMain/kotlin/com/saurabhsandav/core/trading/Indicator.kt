package com.saurabhsandav.core.trading

import java.math.MathContext

interface Indicator<T : Any> {

    val candleSeries: CandleSeries

    val mathContext: MathContext
        get() = candleSeries.indicatorMathContext

    // If null, [CachedIndicator] implementation won't share cache with other instances of itself.
    // Also applies if a non cached [Indicator] with null cacheKey is provided as input to a [CachedIndicator].
    val cacheKey: CacheKey?

    operator fun get(index: Int): T

    interface CacheKey
}
