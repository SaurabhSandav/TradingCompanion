package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow

interface MarketDataProvider {

    fun buildCandleSource(params: StockChartParams): CandleSource

    fun getSymbolTitle(symbolId: SymbolId): Flow<String>

    suspend fun hasVolume(params: StockChartParams): Boolean

    fun sessionChecker(): SessionChecker
}
