package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal class StockChartData(
    val params: StockChartParams,
    val source: CandleSource,
) {

    val coroutineScope = MainScope()
    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    suspend fun getCandleSeries(): CandleSeries = source.getCandleSeries()

    fun getCandleMarkers(): Flow<List<SeriesMarker>> = source.getCandleMarkers()

    fun destroy() {
        coroutineScope.cancel()
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
