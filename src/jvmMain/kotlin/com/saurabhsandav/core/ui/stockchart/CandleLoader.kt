package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.ui.stockchart.StockChartData.LoadState
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

internal class CandleLoader(
    private val marketDataProvider: MarketDataProvider,
    private val onNewDataLoaded: suspend (StockChartParams) -> Unit,
) {

    private val stockChartDataMap = mutableMapOf<StockChartParams, StockChartData>()
    private val loadMutex = Mutex()

    fun getStockChartData(params: StockChartParams): StockChartData {

        // Get or create StockChartData
        return stockChartDataMap.getOrPut(params) {

            val data = StockChartData(
                params = params,
                source = marketDataProvider.buildCandleSource(params),
            )

            // Load initial candles
            data.coroutineScope.launch {

                loadMutex.withLock {

                    data.performLoad { onLoad() }
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
    }

    suspend fun loadBefore(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            getStockChartData(params).performLoad { onLoadBefore() }
        }
    }

    suspend fun loadAfter(params: StockChartParams) {

        // If locked, loading before may be unnecessary.
        if (loadMutex.isLocked) return

        loadMutex.withLock {

            getStockChartData(params).performLoad { onLoadAfter() }
        }
    }

    private suspend fun StockChartData.performLoad(block: suspend CandleSource.() -> Unit) {

        loadState.emit(LoadState.Loading)

        // Perform load in StockChartData coroutineScope.
        // This allows cancellation of all loading for this StockChartData at once.
        // Join so that callers can await load.
        coroutineScope.launch {

            val instantRange = source.getCandleSeries().instantRange.value

            source.block()

            val newInstantRange = source.getCandleSeries().instantRange.value

            if (instantRange != newInstantRange) onNewDataLoaded(params)
        }.join()

        loadState.emit(LoadState.Loaded)
    }
}
