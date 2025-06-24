package com.saurabhsandav.core.trading.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.MathContext
import kotlin.time.Instant

interface CandleSeries : List<Candle> {

    val timeframe: Timeframe

    val live: Flow<IndexedValue<Candle>>

    val instantRange: StateFlow<ClosedRange<Instant>?>

    val modifications: Flow<Pair<ClosedRange<Instant>?, ClosedRange<Instant>?>>

    val indicatorMathContext: MathContext

    // Null [key] values allow caching indicators that shouldn't share cache.
    fun <T> getIndicatorCache(key: Indicator.CacheKey?): IndicatorCache<T>
}
