package com.saurabhsandav.core.trading

import com.saurabhsandav.core.trading.indicator.base.IndicatorCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import java.math.MathContext

interface CandleSeries : List<Candle> {

    val timeframe: Timeframe

    val live: Flow<IndexedValue<Candle>>

    val instantRange: StateFlow<ClosedRange<Instant>?>

    val modifications: Flow<Pair<ClosedRange<Instant>?, ClosedRange<Instant>?>>

    val indicatorMathContext: MathContext

    // Null [key] values allow caching indicators that shouldn't share cache.
    fun <T> getIndicatorCache(key: Indicator.CacheKey?): IndicatorCache<T>
}
