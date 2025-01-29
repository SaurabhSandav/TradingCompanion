package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import kotlinx.coroutines.flow.StateFlow

interface MarketDataProvider {

    fun symbols(): StateFlow<List<String>>

    fun timeframes(): StateFlow<List<Timeframe>>

    fun hasVolume(params: StockChartParams): Boolean

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun releaseCandleSource(candleSource: CandleSource) = Unit

    fun sessionChecker(): SessionChecker
}
