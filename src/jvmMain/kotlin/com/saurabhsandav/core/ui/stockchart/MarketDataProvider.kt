package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal interface MarketDataProvider {

    fun symbols(): StateFlow<ImmutableList<String>>

    fun timeframes(): StateFlow<ImmutableList<Timeframe>>

    fun hasVolume(params: StockChartParams): Boolean

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun releaseCandleSource(candleSource: CandleSource) = Unit

    fun sessionChecker(): SessionChecker
}
