package com.saurabhsandav.core.ui.stockchart

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
import kotlinx.datetime.Instant

class StockChartData(
    val params: StockChartParams,
    val source: CandleSource,
    private val onCandlesLoaded: () -> Unit,
) {

    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    internal var coroutineScope = MainScope()
    private var loadScope = MainScope()

    internal var isCollectingLive = false
        private set

    internal var hasBefore = true
        private set

    internal var hasAfter = true
        private set

    private val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
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

        loadScope.cancel()

        coroutineScope.cancel()
        coroutineScope = MainScope()

        mutableCandleSeries.clear()

        hasBefore = true
        hasAfter = true
    }

    fun destroy() {
        loadScope.cancel()
        coroutineScope.cancel()
    }

    internal fun load(interval: ClosedRange<Instant>): Job = coroutineScope.launch {

        loadScope.cancel()
        loadScope = MainScope()

        loadState.emit(LoadState.Loading)

        // Load
        val result = source.onLoad(interval = interval)

        // Load initial candles
        mutableCandleSeries.replaceCandles(result.candles.first())
        onCandlesLoaded()

        // Candle updates
        result.candles
            .drop(1)
            .filter { candles ->
                candles.size != candleSeries.size
                        || candles.firstOrNull() != candleSeries.firstOrNull()
                        || candles.lastOrNull() != candleSeries.lastOrNull()
            }
            .onEach(mutableCandleSeries::replaceCandles)
            .onEach { onCandlesLoaded() }
            .launchIn(loadScope)

        // Live candles
        result.live
            ?.map { it.value }
            ?.onEach(mutableCandleSeries::addLiveCandle)
            ?.launchIn(loadScope)

        isCollectingLive = result.live != null

        loadState.emit(LoadState.Loaded)
    }

    internal fun setHasBefore(value: Boolean) {
        hasBefore = value
    }

    internal fun setHasAfter(value: Boolean) {
        hasAfter = value
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
