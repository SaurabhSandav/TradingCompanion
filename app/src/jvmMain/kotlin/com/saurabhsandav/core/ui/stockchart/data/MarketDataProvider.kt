package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.trading.core.SessionChecker
import com.saurabhsandav.core.ui.stockchart.StockChartParams

interface MarketDataProvider {

    fun hasVolume(params: StockChartParams): Boolean

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun sessionChecker(): SessionChecker
}
