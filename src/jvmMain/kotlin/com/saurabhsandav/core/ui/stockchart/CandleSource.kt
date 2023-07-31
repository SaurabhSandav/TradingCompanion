package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

internal interface CandleSource {

    val params: StockChartParams

    val hasVolume: Boolean

    val candleSeries: CandleSeries

    suspend fun onLoad()

    suspend fun onLoad(start: Instant, end: Instant? = null) = Unit

    suspend fun onLoadBefore() = Unit

    suspend fun onLoadAfter() = Unit

    fun getCandleMarkers(): Flow<List<SeriesMarker>> = emptyFlow()
}
