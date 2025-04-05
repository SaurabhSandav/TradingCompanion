package com.saurabhsandav.core.ui.stockchart.data

import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlin.math.ceil

internal class StockChartData(
    val source: CandleSource,
    val loadConfig: LoadConfig,
    private val onCandlesLoaded: () -> Unit,
) {

    val params: StockChartParams
        get() = source.params

    val loadState = MutableSharedFlow<LoadState>(replay = 1)

    private var loadScope = MainScope()
    private val loadedPages = LoadedPages()
    private val loadMutex = Mutex()

    private var isInitialized = false
    private var isCollectingLive = false
    private var hasBefore = true
    private var hasAfter = true

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

    fun destroy() {
        loadScope.cancel()
        source.destroy()
    }

    suspend fun loadInitial() {

        if (isInitialized) return

        isInitialized = true

        source.init()
        loadMutex.withLock { loadInitialLockLess() }
    }

    suspend fun loadBefore(loadCount: Int? = null) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Return if all before candles already loaded
            if (!hasBefore) return@withLock

            val initialLoadedInterval = loadedPages.interval
            val loadIterations = when {
                loadCount == null || loadCount <= 0 -> 1
                else -> (loadCount / loadConfig.loadMoreCount.toFloat()).let(::ceil).toInt()
            }

            repeat(loadIterations) {

                val before = loadedPages.start
                val newBefore = source.getBeforeInstant(
                    currentBefore = before,
                    loadCount = loadConfig.loadMoreCount,
                )

                // If no candles available, don't add page.
                if (newBefore == null || before == newBefore) {
                    hasBefore = false
                    return@repeat
                }

                // Add new interval to LoadedPages
                loadedPages.addBefore(newBefore..before)

                // Drop a page if maxCandleCount was crossed
                val maxCandleCount = loadConfig.maxCandleCount ?: return@repeat
                val currentCandleCount = source.getCount(loadedPages.interval)
                if (currentCandleCount > maxCandleCount) loadedPages.dropAfter()
            }

            // Nothing loaded
            if (loadedPages.interval == initialLoadedInterval) return@withLock

            load()
        }
    }

    suspend fun loadAfter(loadCount: Int? = null) {

        // If locked, loading after may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Return if all after candles already loaded
            if (isCollectingLive || !hasAfter) return@withLock

            val initialLoadedInterval = loadedPages.interval
            val loadIterations = when {
                loadCount == null || loadCount <= 0 -> 1
                else -> (loadCount / loadConfig.loadMoreCount.toFloat()).let(::ceil).toInt()
            }

            repeat(loadIterations) {

                val after = loadedPages.endInclusive
                val newAfter = source.getAfterInstant(
                    currentAfter = after,
                    loadCount = loadConfig.loadMoreCount,
                )

                // If no candles available, don't add page.
                if (newAfter == null || after == newAfter) {
                    hasAfter = false
                    return@repeat
                }

                // Add new interval to LoadedPages
                loadedPages.addAfter(after..newAfter)

                // Drop a page if maxCandleCount was crossed
                val maxCandleCount = loadConfig.maxCandleCount ?: return@repeat
                val currentCandleCount = source.getCount(loadedPages.interval)
                if (currentCandleCount > maxCandleCount) loadedPages.dropBefore()
            }

            // Nothing loaded
            if (loadedPages.interval == initialLoadedInterval) return@withLock

            load()
        }
    }

    suspend fun loadInterval(interval: ClosedRange<Instant>) = loadMutex.withLock {

        val currentInterval = loadedPages.interval

        // Return if candles already loaded.
        if (interval.start in currentInterval && interval.endInclusive in currentInterval) return@withLock

        // Candles not already loaded. Replace current candles with given interval candles

        reset()

        var initialLoadInterval = interval
        var initialCandleCount: Int
        var hasBefore = true
        var hasAfter = true

        // If loaded candles count is less than `LoadConfig.initialLoadCount`, load before and after pages
        do {

            val previousInterval = initialLoadInterval

            if (hasBefore) {

                // Get new before
                val newBefore = source.getBeforeInstant(
                    currentBefore = initialLoadInterval.start,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    newBefore == null || initialLoadInterval.start == newBefore -> hasBefore = false
                    else -> initialLoadInterval = newBefore..initialLoadInterval.endInclusive
                }
            }

            if (hasAfter) {

                // Get new after
                val newAfter = source.getAfterInstant(
                    currentAfter = initialLoadInterval.endInclusive,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    newAfter == null || initialLoadInterval.endInclusive == newAfter -> hasAfter = false
                    else -> initialLoadInterval = initialLoadInterval.start..newAfter
                }
            }

            initialCandleCount = source.getCount(initialLoadInterval)

            if (initialLoadInterval == previousInterval) break
        } while (loadConfig.initialLoadCount > initialCandleCount)

        // Add new interval to LoadedPages
        loadedPages.addAfter(initialLoadInterval)

        // If initial candle count is less than initial load count x 2, load more at both ends
        if (initialCandleCount < (loadConfig.initialLoadCount * 2)) {

            // Buffer before
            if (hasBefore) {

                val before = loadedPages.start

                // Get new before
                val newBefore = source.getBeforeInstant(
                    currentBefore = before,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    // If no candles available, do nothing.
                    newBefore == null || before == newBefore -> hasBefore = false
                    // Add new interval to LoadedPages
                    else -> loadedPages.addBefore(newBefore..before)
                }
            }

            // Buffer after
            if (hasAfter) {

                val after = loadedPages.endInclusive

                // Get new after
                val newAfter = source.getAfterInstant(
                    currentAfter = after,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    // If no candles available, do nothing.
                    newAfter == null || after == newAfter -> hasAfter = false
                    // Add new interval to LoadedPages
                    else -> loadedPages.addAfter(after..newAfter)
                }
            }
        }

        this.hasBefore = hasBefore
        this.hasAfter = hasAfter
        load()
    }

    suspend fun loadLatest() = loadMutex.withLock {

        // Return if all latest candles already loaded
        if (isCollectingLive || !hasAfter) return@withLock

        val initialLoadBefore = loadConfig.initialLoadBefore()

        // Return if initial interval is already loaded
        if (loadedPages.endInclusive >= initialLoadBefore) return@withLock

        // Reset candles and load initial interval
        reset()
        loadInitialLockLess()
    }

    private suspend fun loadInitialLockLess() {

        val initialLoadBefore = loadConfig.initialLoadBefore()
        val newBefore = source.getBeforeInstant(
            currentBefore = initialLoadBefore,
            loadCount = loadConfig.initialLoadCount,
        )

        // If no candles available, don't add page.
        if (newBefore == null || initialLoadBefore == newBefore) return

        // Add initial page
        loadedPages.addAfter(newBefore..initialLoadBefore)

        load()
    }

    /** Loads entire interval again and restarts live candle collection */
    private suspend fun load() {

        // Recreate Candle update/live scope
        loadScope.cancel()
        loadScope = MainScope()

        loadState.emit(LoadState.Loading)

        // Load
        val result = source.onLoad(interval = loadedPages.interval)

        // Load initial candles
        mutableCandleSeries.replaceCandles(result.candles.first())
        onCandlesLoaded()

        // Candle updates
        result.candles
            .drop(1)
            .filter { candles ->
                candles.size != candleSeries.size ||
                    candles.firstOrNull() != candleSeries.firstOrNull() ||
                    candles.lastOrNull() != candleSeries.lastOrNull()
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

    private fun reset() {

        loadScope.cancel()

        loadedPages.clear()
        mutableCandleSeries.clear()

        hasBefore = true
        hasAfter = true
    }

    enum class LoadState {
        Loading,
        Loaded,
    }
}
