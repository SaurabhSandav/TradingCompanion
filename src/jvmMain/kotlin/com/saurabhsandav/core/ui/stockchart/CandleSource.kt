package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

interface CandleSource {

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

    fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> = emptyFlow()

    fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> = emptyFlow()
}
