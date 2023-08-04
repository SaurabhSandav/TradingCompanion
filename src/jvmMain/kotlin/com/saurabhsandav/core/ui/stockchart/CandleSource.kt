package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

internal interface CandleSource {

    val params: StockChartParams

    /**
     * Get CandleSeries. Suspends until CandleSeries is available. Does not load any candles.
     * May suspend until initial candle load.
     */
    suspend fun getCandleSeries(): CandleSeries

    suspend fun onLoad()

    suspend fun onLoad(
        instant: Instant,
        to: Instant? = null,
        bufferCount: Int? = null,
    ) = Unit

    suspend fun onLoadBefore() = Unit

    suspend fun onLoadAfter() = Unit

    fun getCandleMarkers(): Flow<List<SeriesMarker>> = emptyFlow()
}
