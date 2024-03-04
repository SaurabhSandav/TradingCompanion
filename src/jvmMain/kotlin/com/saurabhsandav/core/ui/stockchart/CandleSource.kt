package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

interface CandleSource {

    val params: StockChartParams

    suspend fun onLoad(
        interval: ClosedRange<Instant>,
    ): Result

    suspend fun onLoadBefore(
        before: Instant,
        count: Int,
    ): Result

    suspend fun onLoadAfter(
        after: Instant,
        count: Int,
    ): Result

    fun getTradeMarkers(
        instantRange: ClosedRange<Instant>,
    ): Flow<List<TradeMarker>> = emptyFlow()

    fun getTradeExecutionMarkers(
        instantRange: ClosedRange<Instant>,
    ): Flow<List<TradeExecutionMarker>> = emptyFlow()

    class Result(
        val candles: List<Candle>,
        val live: Flow<IndexedValue<Candle>>? = null,
    )
}
