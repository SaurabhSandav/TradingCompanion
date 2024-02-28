package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import kotlinx.coroutines.flow.StateFlow

interface MarketDataProvider {

    fun symbols(): StateFlow<List<String>>

    fun timeframes(): StateFlow<List<Timeframe>>

    fun hasVolume(params: StockChartParams): Boolean

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun releaseCandleSource(candleSource: CandleSource) = Unit

    fun sessionChecker(): SessionChecker
}
