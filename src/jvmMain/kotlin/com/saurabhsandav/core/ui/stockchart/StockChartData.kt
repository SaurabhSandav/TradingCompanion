package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant

class StockChartData(
    val params: StockChartParams,
    val source: CandleSource,
) {

    val coroutineScope = MainScope()
    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    suspend fun getCandleSeries(): CandleSeries = source.getCandleSeries()

    fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {
        return source.getTradeMarkers(instantRange)
    }

    fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> {
        return source.getTradeExecutionMarkers(instantRange)
    }

    fun destroy() {
        coroutineScope.cancel()
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
