package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.trading.core.Candle
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Instant

interface CandleSource {

    val params: StockChartParams

    suspend fun init() = Unit

    fun destroy() = Unit

    suspend fun onLoad(interval: ClosedRange<Instant>): Result

    suspend fun getCount(interval: ClosedRange<Instant>): Int

    suspend fun getBeforeInstant(
        currentBefore: Instant,
        loadCount: Int,
    ): Instant?

    suspend fun getAfterInstant(
        currentAfter: Instant,
        loadCount: Int,
    ): Instant?

    fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> = emptyFlow()

    fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> = emptyFlow()

    class Result(
        val candles: Flow<List<Candle>>,
        val live: Flow<IndexedValue<Candle>>? = null,
    )
}
