package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.arrangement.PagedChartArrangement
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@Stable
internal class StockChartsState(
    private val initialParams: StockChartParams,
    val marketDataProvider: MarketDataProvider,
    private val appModule: AppModule,
    val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val coroutineScope = MainScope()
    private val isDark = appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)
    private val lastActiveChart = MutableStateFlow<StockChart?>(null)
    private val candleSources = mutableMapOf<StockChartParams, CandleSource>()

    val windows = mutableStateListOf<StockChartWindow>()
    val charts
        get() = windows.flatMap { it.charts }

    init {

        newWindow(null)

        // Setting dark mode according to settings
        coroutineScope.launch {
            isDark.collect { isDark ->
                charts.forEach { it.setDarkMode(isDark) }
            }
        }
    }

    fun newWindow(fromStockChart: StockChart?) {

        val window = StockChartWindow(
            onNewChart = { arrangement, currentStockChart ->
                newStockChart(arrangement, (currentStockChart ?: fromStockChart)?.params)
            },
            onSelectChart = { stockChart ->

                // Set selected chart as lastActiveChart
                lastActiveChart.value = stockChart
            },
            onCloseChart = { stockChart ->

                // Destroy chart
                stockChart.destroy()

                // Remove unused CandleSources from cache
                releaseUnusedCandleSources()
            },
        )

        windows += window

        // Update last active chart
        window.pagedArrangement.lastActiveChart
            .onEach { actualChart -> lastActiveChart.value = window.charts.first { it.actualChart == actualChart } }
            .launchIn(window.coroutineScope)
    }

    fun closeWindow(window: StockChartWindow): Boolean {

        if (windows.size == 1) return false

        windows.remove(window)

        window.coroutineScope.cancel()

        return true
    }

    fun newChart(params: StockChartParams): StockChart {

        // Get last active chart
        val lastActiveChart = checkNotNull(lastActiveChart.value) { "No last active chart" }

        // Get last active window based on last active chart
        val window = windows.first { lastActiveChart in it.charts }

        // Create new StockChart
        val stockChart = newStockChart(window.pagedArrangement, params)

        // Add our new chart to the window
        window.openChart(stockChart)

        return stockChart
    }

    fun bringToFront(stockChart: StockChart) {

        val window = windows.first { stockChart in it.charts }

        // Bring window to front
        window.appWindowState.toFront()

        // Select tab
        window.tabCharts.forEach { (tabId, chart) ->

            if (stockChart == chart) {
                window.tabsState.selectTab(tabId)
                return@forEach
            }
        }
    }

    fun onChangeTicker(stockChart: StockChart, ticker: String) {

        // New chart params
        stockChart.newParams(stockChart.params.copy(ticker = ticker))
    }

    fun onChangeTimeframe(stockChart: StockChart, timeframe: Timeframe) {

        // New chart params
        stockChart.newParams(stockChart.params.copy(timeframe = timeframe))
    }

    fun goToDateTime(
        stockChart: StockChart,
        dateTime: LocalDateTime?,
    ) = coroutineScope.launchUnit {

        val instant = dateTime?.toInstant(TimeZone.currentSystemDefault())

        // Load data if date specified
        if (instant != null)
            charts.map { it.loadInterval(instant) }.awaitAll()

        // Navigate to datetime, other charts should be synced to same datetime.
        stockChart.goToDateTime(dateTime)
    }

    private fun newStockChart(
        arrangement: PagedChartArrangement,
        params: StockChartParams?,
    ): StockChart {

        // New chart
        val actualChart = arrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        // New StockChart
        val stockChart = StockChart(
            appModule = appModule,
            marketDataProvider = marketDataProvider,
            actualChart = actualChart,
            initialSource = getCandleSource(params ?: initialParams),
            onLegendUpdate = { arrangement.setLegend(actualChart, it) },
        )

        // Initial theme
        stockChart.setDarkMode(isDark.value)

        // Sync visible range across charts
        actualChart.timeScale
            .visibleLogicalRangeChange()
            .filterNotNull()
            .onEach { logicalRange ->

                // If chart is not active (user hasn't interacted), skip sync
                if (lastActiveChart.value != stockChart) return@onEach

                // Current instant range. If not populated, skip sync
                val instantRange = stockChart.source.candleSeries.instantRange.value ?: return@onEach

                // Update all other charts with same timeframe
                charts
                    .filter { filterStockChart ->
                        // Select charts with same timeframe, ignore current chart
                        stockChart.params.timeframe == filterStockChart.params.timeframe &&
                                filterStockChart != stockChart
                    }
                    .forEach { chart ->

                        // Chart instant range. If not populated, skip sync
                        val chartInstantRange = chart.source.candleSeries.instantRange.value ?: return@forEach

                        // Intersection range of current chart and iteration chart
                        // Skip sync if there is no overlap in instant ranges
                        val intersection = instantRange.intersect(chartInstantRange) ?: return@forEach

                        // Pick a common candle instant to use for calculating a sync offset
                        val commonInstant = intersection.endInclusive

                        // Current chart common candle index
                        val candleIndex = stockChart.source.candleSeries.binarySearch {
                            it.openInstant.compareTo(commonInstant)
                        }
                        // Iteration chart common candle index
                        val chartCandleIndex = chart.source.candleSeries.binarySearch {
                            it.openInstant.compareTo(commonInstant)
                        }

                        // Sync offset for iteration chart
                        val syncOffset = (chartCandleIndex - candleIndex).toFloat()

                        // Set logical range with calculated offset
                        chart.actualChart.timeScale.setVisibleLogicalRange(
                            from = logicalRange.from + syncOffset,
                            to = logicalRange.to + syncOffset,
                        )
                    }
            }
            .launchIn(coroutineScope)

        return stockChart
    }

    private fun getCandleSource(params: StockChartParams): CandleSource {
        return candleSources.getOrPut(params) {
            marketDataProvider.buildCandleSource(params)
        }
    }

    private fun StockChart.newParams(params: StockChartParams) {

        val candleSource = getCandleSource(params)

        // Set CandleSource on StockChart
        setCandleSource(candleSource)

        // Remove unused CandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun releaseUnusedCandleSources() = coroutineScope.launchUnit {

        // CandleSources currently in use
        val usedCandleSources = charts.map { stockChart -> stockChart.source }

        // CandleSources not in use
        val unusedCandleSources = candleSources.filter { (_, candleSource) -> candleSource !in usedCandleSources }

        // Remove unused CandleSource from cache
        unusedCandleSources.forEach { (params, candleSource) ->

            // Remove from cache
            candleSources.remove(params)

            // Notify MarketDataProvider about candle source release
            marketDataProvider.releaseCandleSource(candleSource)
        }
    }

    private fun ClosedRange<Instant>.intersect(other: ClosedRange<Instant>): ClosedRange<Instant>? {

        // Check if the ranges intersect
        val intersects = start <= other.endInclusive && endInclusive >= other.start

        // If ranges don't intersect, return null
        if (!intersects) return null

        // Calculate the start and end of the intersection range
        val start = maxOf(start, other.start)
        val end = minOf(endInclusive, other.endInclusive)

        // Intersection range
        return start..end
    }
}
