package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.Timeframe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

class CandleLoader(
    private val marketDataProvider: MarketDataProvider,
    val loadConfig: LoadConfig,
    private val onCandlesLoaded: (StockChartParams) -> Unit,
) {

    private val stockChartDataMap = mutableMapOf<StockChartParams, StockChartData>()
    private val loadedPagesMap = mutableMapOf<Timeframe, LoadedPages>()
    private val loadMutex = Mutex()

    fun getStockChartData(params: StockChartParams): StockChartData {

        // Get or create StockChartData
        return stockChartDataMap.getOrPut(params) {

            val data = StockChartData(
                params = params,
                source = marketDataProvider.buildCandleSource(params),
                onCandlesLoaded = { onCandlesLoaded(params) },
            )

            data.coroutineScope.launch {

                loadMutex.withLock {

                    loadInitial(data)
                }
            }

            return@getOrPut data
        }
    }

    fun releaseStockChartData(params: StockChartParams) {

        // Remove StockChartData from cache
        val data = stockChartDataMap.remove(params)

        if (data != null) {

            // Destroy StockChartData
            data.destroy()

            // Notify MarketDataProvider about CandleSource release
            marketDataProvider.releaseCandleSource(data.source)
        }
    }

    suspend fun reset() = loadMutex.withLock {

        loadedPagesMap.clear()

        coroutineScope {

            // Initial load must not happen concurrently.
            // It can lead to a race condition when creating initial LoadedPages instance.
            // Alternatively we can pick a StockChartData for initial LoadedPages creation and then load the rest
            // of StockChartData(s) concurrently.
            stockChartDataMap.values.forEach { data ->
                data.reset()
                loadInitial(data)
            }
        }
    }

    suspend fun load(
        params: StockChartParams,
        interval: ClosedRange<Instant>,
    ) = loadMutex.withLock {

        // Already loaded interval for timeframe
        val loadedPages = loadedPagesMap[params.timeframe]!!
        val currentInterval = loadedPages.interval

        // Return if candles already loaded.
        if (interval.start in currentInterval && interval.endInclusive in currentInterval) return@withLock

        // Candles not already loaded. Replace current candles with given interval candles

        val data = getStockChartData(params)

        var initialLoadInterval = interval
        var initialCandleCount: Int
        var hasBefore = true
        var hasAfter = true

        // If loaded candles count is less than `LoadConfig.initialLoadCount`, load before and after pages
        do {

            val previousInterval = initialLoadInterval

            if (hasBefore) {

                // Get new before
                val newBefore = data.source.getBeforeInstant(
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
                val newAfter = data.source.getAfterInstant(
                    currentAfter = initialLoadInterval.endInclusive,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    newAfter == null || initialLoadInterval.endInclusive == newAfter -> hasAfter = false
                    else -> initialLoadInterval = initialLoadInterval.start..newAfter
                }
            }

            initialCandleCount = data.source.getCount(initialLoadInterval)

            if (initialLoadInterval == previousInterval) break

        } while (loadConfig.initialLoadCount > initialCandleCount)

        // Add new interval to LoadedPages
        val newLoadedPages = LoadedPages(initialPage = initialLoadInterval)
        loadedPagesMap[params.timeframe] = newLoadedPages

        // If initial candle count is less than initial load count x 2, load more at both ends
        if (initialCandleCount < (loadConfig.initialLoadCount * 2)) {

            // Buffer before
            if (hasBefore) {

                val before = newLoadedPages.start

                // Get new before
                val newBefore = data.source.getBeforeInstant(
                    currentBefore = before,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    // If no candles available, do nothing.
                    newBefore == null || before == newBefore -> hasBefore = false
                    // Add new interval to LoadedPages
                    else -> newLoadedPages.addBefore(newBefore..before)
                }
            }

            // Buffer after
            if (hasAfter) {

                val after = newLoadedPages.endInclusive

                // Get new after
                val newAfter = data.source.getAfterInstant(
                    currentAfter = after,
                    loadCount = loadConfig.loadMoreCount,
                )

                when {
                    // If no candles available, do nothing.
                    newAfter == null || after == newAfter -> hasAfter = false
                    // Add new interval to LoadedPages
                    else -> newLoadedPages.addAfter(after..newAfter)
                }
            }
        }

        // Sync other StockChartData with same timeframe
        stockChartDataMap
            .keys
            .filter { it.timeframe == params.timeframe }
            .map { mapParams ->
                getStockChartData(mapParams).run {
                    reset()
                    if (data == this) {
                        setHasBefore(hasBefore)
                        setHasAfter(hasAfter)
                    }
                    load(newLoadedPages.interval)
                }
            }
            .joinAll()
    }

    suspend fun loadBefore(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            val data = getStockChartData(params)

            if (!data.hasBefore) return@withLock

            // LoadedPages for timeframe
            val loadedPages = loadedPagesMap[params.timeframe]!!
            // Start of loaded interval for current timeframe
            val before = loadedPages.start

            // Get new before
            val newBefore = data.source.getBeforeInstant(
                currentBefore = before,
                loadCount = loadConfig.loadMoreCount,
            )

            // If no candles available, do nothing.
            if (newBefore == null || before == newBefore) {
                data.setHasBefore(false)
                return@withLock
            }

            // Add new interval to LoadedPages
            loadedPages.addBefore(newBefore..before)

            // Sync other StockChartData with same timeframe
            stockChartDataMap
                .keys
                .filter { it.timeframe == params.timeframe }
                .map { mapParams -> getStockChartData(mapParams).load(loadedPages.interval) }
                .joinAll()
        }
    }

    suspend fun loadAfter(params: StockChartParams) {

        // If locked, loading after may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            val data = getStockChartData(params)

            // Don't try to load after if collecting live candles
            if (data.isCollectingLive) return@withLock

            if (!data.hasAfter) return@withLock

            // LoadedPages for timeframe
            val loadedPages = loadedPagesMap[params.timeframe]!!
            // End of loaded interval for current timeframe
            val after = loadedPages.endInclusive

            // Get new after
            val newAfter = data.source.getAfterInstant(
                currentAfter = after,
                loadCount = loadConfig.loadMoreCount,
            )

            // If no candles available, do nothing.
            if (newAfter == null || after == newAfter) {
                data.setHasAfter(false)
                return@withLock
            }

            // Add new interval to LoadedPages
            loadedPages.addAfter(after..newAfter)

            // Sync other StockChartData with same timeframe
            stockChartDataMap
                .keys
                .filter { it.timeframe == params.timeframe }
                .map { mapParams -> getStockChartData(mapParams).load(loadedPages.interval) }
                .joinAll()
        }
    }

    private suspend fun loadInitial(data: StockChartData) {

        val loadedPages = loadedPagesMap.getOrPut(data.params.timeframe) {

            val newBefore = data.source.getBeforeInstant(
                currentBefore = loadConfig.initialLoadBefore,
                loadCount = loadConfig.initialLoadCount,
            )

            // If no candles available, don't add page.
            if (newBefore == null || loadConfig.initialLoadBefore == newBefore) return

            // Create LoadedPages with initial page
            LoadedPages(initialPage = newBefore..loadConfig.initialLoadBefore)
        }

        data.load(loadedPages.interval).join()
    }

    private class LoadedPages(initialPage: ClosedRange<Instant>) {

        val pages = mutableListOf(initialPage)

        val start: Instant
            get() = pages.first().start

        val endInclusive: Instant
            get() = pages.last().endInclusive

        val interval: ClosedRange<Instant>
            get() = start..endInclusive

        fun addBefore(range: ClosedRange<Instant>) {
            pages.add(0, range)
        }

        fun addAfter(range: ClosedRange<Instant>) {
            pages.add(range)
        }
    }
}
