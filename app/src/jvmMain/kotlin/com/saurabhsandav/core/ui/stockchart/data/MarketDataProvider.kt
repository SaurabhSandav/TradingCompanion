package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.trading.core.SessionChecker

interface MarketDataProvider {

    fun hasVolume(params: StockChartParams): Boolean

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun sessionChecker(): SessionChecker
}
