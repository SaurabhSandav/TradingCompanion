package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockChartData(
    val params: StockChartParams,
    val source: CandleSource,
) {

    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    internal var coroutineScope = MainScope()
        private set

    internal var liveJob: Job? = null
        private set

    internal val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
    val candleSeries = mutableCandleSeries.asCandleSeries()

    val tradeMarkers: Flow<List<TradeMarker>> = flow {

        candleSeries.instantRange.flatMapLatest { instantRange ->
            when (instantRange) {
                null -> flowOf(emptyList())
                else -> source.getTradeMarkers(instantRange)
            }
        }.emitInto(this)
    }

    val tradeExecutionMarkers: Flow<List<TradeExecutionMarker>> = flow {

        candleSeries.instantRange.flatMapLatest { instantRange ->
            when (instantRange) {
                null -> flowOf(emptyList())
                else -> source.getTradeExecutionMarkers(instantRange)
            }
        }.emitInto(this)
    }

    fun reset() {

        coroutineScope.cancel()
        coroutineScope = MainScope()

        stopCollectingLive()

        mutableCandleSeries.clear()
    }

    fun destroy() {
        coroutineScope.cancel()
        liveJob?.cancel()
    }

    internal fun collectLive(live: Flow<Candle>) {

        if (liveJob != null) return

        liveJob = coroutineScope.launch {
            live.collect(mutableCandleSeries::addLiveCandle)
        }
    }

    internal fun stopCollectingLive() {
        liveJob?.cancel()
        liveJob = null
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
