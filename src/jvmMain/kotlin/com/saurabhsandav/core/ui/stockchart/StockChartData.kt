package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class StockChartData(
    val params: StockChartParams,
    val source: CandleSource,
) {

    val coroutineScope = MainScope()
    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    suspend fun getCandleSeries(): CandleSeries = source.getCandleSeries()

    val tradeMarkers: Flow<List<TradeMarker>> = flow {

        val flow = getCandleSeries().instantRange.flatMapLatest { instantRange ->
            when (instantRange) {
                null -> flowOf(emptyList())
                else -> source.getTradeMarkers(instantRange)
            }
        }

        emitAll(flow)
    }

    val tradeExecutionMarkers: Flow<List<TradeExecutionMarker>> = flow {

        val flow = getCandleSeries().instantRange.flatMapLatest { instantRange ->
            when (instantRange) {
                null -> flowOf(emptyList())
                else -> source.getTradeExecutionMarkers(instantRange)
            }
        }

        emitAll(flow)
    }

    fun destroy() {
        coroutineScope.cancel()
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
