package com.saurabhsandav.core.trading

import com.saurabhsandav.core.trading.indicator.base.IndicatorCache
import kotlinx.coroutines.flow.Flow
import java.math.MathContext

interface CandleSeries : List<Candle> {

    val timeframe: Timeframe

    val live: Flow<Candle>

    val indicatorMathContext: MathContext

    fun <T> getIndicatorCache(key: String?): IndicatorCache<T>
}
