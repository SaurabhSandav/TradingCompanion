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

internal class CandleLoader(
    private val marketDataProvider: MarketDataProvider,
) {

    private val stockChartDataMap = mutableMapOf<StockChartParams, StockChartData>()
    private val loadedIntervals = mutableMapOf<Timeframe, ClosedRange<Instant>>()
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

                    data.performLoad {

                        // Load initial candles
                        onLoad()

                        // Previously loaded interval for same timeframe
                        val loadedInterval = loadedIntervals[params.timeframe]

                        when {
                            // Match load with previously loaded interval
                            loadedInterval != null -> onLoad(
                                instant = loadedInterval.start,
                                to = loadedInterval.endInclusive,
                            )

                            else -> loadedIntervals[params.timeframe] = data.getCandleSeries().instantRange.value!!
                        }
                    }
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

    suspend fun load(
        params: StockChartParams,
        instant: Instant,
        to: Instant? = null,
    ) = loadMutex.withLock {

        // Load candles
        getStockChartData(params).performLoad {

            onLoad(
                instant = instant,
                to = to,
                // In case this method is called before navigating to the given interval, bufferCount should be greater
                // than the 'load more threshold'. Otherwise, it'll trigger a load more request right after the
                // navigation is complete.
                bufferCount = StockChartLoadInstantBuffer,
            )
        }

        // Sync load interval for other StockChartData with same timeframe
        syncLoadIntervalsAcrossTimeframe(params)
    }

    suspend fun loadBefore(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Load candles
            getStockChartData(params).performLoad { onLoadBefore() }

            // Sync load interval for other StockChartData with same timeframe
            syncLoadIntervalsAcrossTimeframe(params)
        }
    }

    suspend fun loadAfter(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            // Load candles
            getStockChartData(params).performLoad { onLoadAfter() }

            // Sync load interval for other StockChartData with same timeframe
            syncLoadIntervalsAcrossTimeframe(params)
        }
    }

    private suspend fun StockChartData.performLoad(block: suspend CandleSource.() -> Unit) {

        loadState.emit(LoadState.Loading)

        // Perform load in StockChartData coroutineScope.
        // This allows cancellation of all loading for this StockChartData at once.
        // Join so that callers can await load.
        coroutineScope.launch { source.block() }.join()

        loadState.emit(LoadState.Loaded)
    }

    private suspend fun syncLoadIntervalsAcrossTimeframe(params: StockChartParams) = coroutineScope {

        val candleSeries = getStockChartData(params).getCandleSeries()

        // Update loaded interval
        loadedIntervals[params.timeframe] = candleSeries.instantRange.value!!

        stockChartDataMap
            .keys
            .filter { it.timeframe == params.timeframe }
            .map { otherParams ->

                async {

                    getStockChartData(otherParams).performLoad {

                        onLoad(
                            instant = candleSeries.first().openInstant,
                            to = candleSeries.last().openInstant,
                        )
                    }
                }
            }
            .awaitAll()
    }
}
