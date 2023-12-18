package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.StockChartData.LoadState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

class CandleLoader(
    private val marketDataProvider: MarketDataProvider,
    private val loadConfig: LoadConfig,
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

        val existingRange = loadedPages.pages.run { first().start..last().endInclusive }

        // Candles already loaded. Return.
        if (interval.start in existingRange && interval.endInclusive in existingRange) return@withLock

        // Candles not already loaded. Replace current candles with given interval candles
        getStockChartData(params).performLoad {

            // Load initial candles
            val result = source.onLoad(interval)

            // If no candles available, do nothing.
            if (result.candles.isNotEmpty()) {

                // Stop collecting live candles
                stopCollectingLive()

                // Replace all candles with newly loaded
                mutableCandleSeries.clear()
                mutableCandleSeries.appendCandles(result.candles)

                // Collect live candles if available
                if (result.live != null) collectLive(result.live)

                // Create new LoadedPages
                val newLoadedPages = LoadedPages(interval)
                loadedPagesMap[params.timeframe] = newLoadedPages

                // If loaded candles count is less than `LoadConfig.initialLoadCount`, load before and after pages
                val shortfall = loadConfig.initialLoadCount - candleSeries.size
                if (shortfall > 0) {

                    // Load before
                    val beforeResult = source.onLoadBefore(
                        before = interval.start,
                        count = loadConfig.loadMoreCount,
                    )

                    if (beforeResult.candles.isNotEmpty()) {

                        mutableCandleSeries.prependCandles(beforeResult.candles)

                        // Add before page to LoadedPages
                        newLoadedPages.addBefore(beforeResult.candles.first().openInstant..interval.start)
                    }

                    // Load after page only if live candle collection is not ongoing
                    if (liveJob == null) {

                        // Load after
                        val afterResult = source.onLoadAfter(
                            after = interval.endInclusive,
                            count = loadConfig.loadMoreCount,
                        )

                        if (afterResult.candles.isNotEmpty()) {

                            mutableCandleSeries.appendCandles(afterResult.candles)

                            // Add after page to LoadedPages
                            newLoadedPages.addAfter(interval.endInclusive..afterResult.candles.last().openInstant)
                        }

                        // Collect live candles if available
                        if (afterResult.live != null) collectLive(afterResult.live)
                    }
                }

                val requestInterval = newLoadedPages.pages.run { first().start..last().endInclusive }

                // Sync other StockChartData with same timeframe
                coroutineScope {

                    stockChartDataMap
                        .keys
                        // Skip already loaded StockChartData
                        .filter { it.timeframe == params.timeframe && it != params }
                        .map { mapParams ->

                            async {

                                getStockChartData(mapParams).performLoad {

                                    // Load interval
                                    val mapResult = source.onLoad(requestInterval)

                                    // Stop collecting live candles
                                    stopCollectingLive()

                                    // Replace all candles with newly loaded
                                    mutableCandleSeries.clear()
                                    mutableCandleSeries.appendCandles(mapResult.candles)

                                    // Collect live candles if available
                                    if (mapResult.live != null) collectLive(mapResult.live)
                                }
                            }
                        }
                        .awaitAll()
                }
            }
        }
    }

    suspend fun loadBefore(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Already loaded interval for timeframe
            val loadedPages = loadedPagesMap[params.timeframe]!!
            // Start of loaded interval for current timeframe
            val before = loadedPages.pages.first().start

            getStockChartData(params).performLoad {

                // Load before
                val result = source.onLoadBefore(
                    before = before,
                    count = loadConfig.loadMoreCount,
                )

                // If no candles available, don't add page. If no page being added, don't collect live candles.
                if (result.candles.isNotEmpty()) {

                    mutableCandleSeries.prependCandles(result.candles)

                    // Request interval considered from first of loaded candles to the before value
                    val requestInterval = result.candles.first().openInstant..before

                    // Add loaded interval to LoadedPages
                    loadedPages.addBefore(requestInterval)

                    // Sync other StockChartData with same timeframe
                    coroutineScope {

                        stockChartDataMap
                            .keys
                            // Skip already loaded StockChartData
                            .filter { it.timeframe == params.timeframe && it != params }
                            .map { mapParams ->

                                async {

                                    getStockChartData(mapParams).performLoad {

                                        // Load interval
                                        val mapResult = source.onLoad(requestInterval)

                                        mutableCandleSeries.prependCandles(mapResult.candles)
                                    }
                                }
                            }
                            .awaitAll()
                    }
                }
            }
        }
    }

    suspend fun loadAfter(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Already loaded interval for timeframe
            val loadedPages = loadedPagesMap[params.timeframe]!!
            // End of loaded interval for current timeframe
            val after = loadedPages.pages.last().endInclusive

            getStockChartData(params).performLoad {

                // Don't try to load after if live candles collection ongoing
                if (liveJob != null) return@performLoad

                // Load after
                val result = source.onLoadAfter(
                    after = after,
                    count = loadConfig.loadMoreCount,
                )

                // If no candles available, don't add page. If no page being added, don't collect live candles.
                if (result.candles.isNotEmpty()) {

                    mutableCandleSeries.appendCandles(result.candles)

                    // Collect live candles if available
                    if (result.live != null) collectLive(result.live)

                    // Request interval considered from after value to the last of loaded values
                    val requestInterval = after..result.candles.last().openInstant

                    // Add loaded interval to LoadedPages
                    loadedPages.addAfter(requestInterval)

                    // Sync other StockChartData with same timeframe
                    coroutineScope {

                        stockChartDataMap
                            .keys
                            // Skip already loaded StockChartData
                            .filter { it.timeframe == params.timeframe && it != params }
                            .map { mapParams ->

                                async {

                                    getStockChartData(mapParams).performLoad mapLoad@{

                                        // Don't try to load after if collecting live candles
                                        if (liveJob != null) return@mapLoad

                                        // Load interval
                                        val mapResult = source.onLoad(requestInterval)

                                        mutableCandleSeries.appendCandles(mapResult.candles)

                                        // Collect live candles if available
                                        if (mapResult.live != null) collectLive(mapResult.live)
                                    }
                                }
                            }
                            .awaitAll()
                    }
                }
            }
        }
    }

    private suspend fun loadInitial(data: StockChartData) {

        data.performLoad {

            // Previously loaded interval for same timeframe
            when (val loadedPages = loadedPagesMap[params.timeframe]) {
                // Initial page load for this timeframe
                null -> {

                    // Load initial candles
                    val result = source.onLoadBefore(
                        before = loadConfig.initialLoadBefore,
                        count = loadConfig.initialLoadCount,
                    )

                    // If no candles available, don't add page. If no page being added, don't collect live candles.
                    if (result.candles.isNotEmpty()) {

                        mutableCandleSeries.appendCandles(result.candles)

                        // Collect live candles if available
                        if (result.live != null) collectLive(result.live)

                        // Create LoadedPages with initial page
                        loadedPagesMap[params.timeframe] = LoadedPages(
                            initialPage = result.candles.first().openInstant..loadConfig.initialLoadBefore
                        )
                    }
                }

                // Timeframe already has loaded pages
                else -> {

                    // Already loaded interval for timeframe
                    val requestInterval = loadedPages.pages.run { first().start..last().endInclusive }

                    // Load
                    val result = source.onLoad(interval = requestInterval)

                    mutableCandleSeries.appendCandles(result.candles)

                    // Collect live candles if available
                    if (result.live != null) collectLive(result.live)
                }
            }
        }
    }

    private suspend fun StockChartData.performLoad(block: suspend StockChartData.() -> Unit) {

        loadState.emit(LoadState.Loading)

        // Perform load in StockChartData coroutineScope.
        // This allows cancellation of all loading for this StockChartData at once.
        // Join so that callers can await load.
        coroutineScope.launch { block() }.join()

        loadState.emit(LoadState.Loaded)
    }

    private class LoadedPages(initialPage: ClosedRange<Instant>) {

        val pages = mutableListOf(initialPage)

        fun addBefore(range: ClosedRange<Instant>) {
            pages.add(0, range)
        }

        fun addAfter(range: ClosedRange<Instant>) {
            pages.add(range)
        }
    }
}
