package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions.CrosshairMode
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.arrangement.PagedChartArrangement
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.prefs.Preferences

@Stable
class StockChartsState(
    parentScope: CoroutineScope,
    private val initialParams: StockChartParams,
    val marketDataProvider: MarketDataProvider,
    val appPrefs: FlowSettings,
    val webViewStateProvider: () -> WebViewState,
) {

    private val chartPrefs = PreferencesSettings(Preferences.userRoot().node(AppPaths.appName).node("StockChart"))
        .toFlowSettings()

    private val coroutineScope = parentScope.newChildScope()
    private val isDark = appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)
    private val lastActiveChart = MutableStateFlow<StockChart?>(null)

    val windows = mutableStateListOf<StockChartWindow>()
    val charts
        get() = windows.flatMap { it.charts }
    private val candleLoader = CandleLoader(marketDataProvider)

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
            parentScope = coroutineScope,
            webViewState = webViewStateProvider(),
            onNewChart = { arrangement, currentStockChart ->

                newStockChart(
                    arrangement = arrangement,
                    params = (currentStockChart ?: fromStockChart)?.params,
                    initialVisibleRange = (currentStockChart ?: fromStockChart)?.visibleRange,
                )
            },
            onSelectChart = { stockChart ->

                // Set selected chart as lastActiveChart
                lastActiveChart.value = stockChart
            },
            onCloseChart = { stockChart ->

                // Destroy chart
                stockChart.destroy()

                // Release StockChartData if unused
                if (!charts.any { it.params == stockChart.params })
                    candleLoader.releaseStockChartData(stockChart.params)
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
        window.toFront()

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

        // Navigate to datetime
        stockChart.navigateTo(instant = dateTime?.toInstant(TimeZone.currentSystemDefault()))
    }

    private fun newStockChart(
        arrangement: PagedChartArrangement,
        params: StockChartParams?,
        initialVisibleRange: ClosedRange<Float>? = null,
    ): StockChart {

        // New chart
        val actualChart = arrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        // New StockChart
        val stockChart = StockChart(
            parentScope = coroutineScope,
            prefs = chartPrefs,
            marketDataProvider = marketDataProvider,
            candleLoader = candleLoader,
            actualChart = actualChart,
            initialData = candleLoader.getStockChartData(params ?: initialParams),
            initialVisibleRange = initialVisibleRange,
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
                val instantRange = stockChart.data.getCandleSeries().instantRange.value ?: return@onEach

                // Update all other charts with same timeframe
                charts
                    .filter { filterStockChart ->
                        // Select charts with same timeframe, ignore current chart
                        stockChart.params.timeframe == filterStockChart.params.timeframe &&
                                filterStockChart != stockChart
                    }
                    .forEach { chart ->

                        // Chart instant range. If not populated, skip sync
                        val chartInstantRange = chart.data.getCandleSeries().instantRange.value ?: return@forEach

                        // Intersection range of current chart and iteration chart
                        // Skip sync if there is no overlap in instant ranges
                        val intersection = instantRange.intersect(chartInstantRange) ?: return@forEach

                        // Pick a common candle instant to use for calculating a sync offset
                        val commonInstant = intersection.endInclusive

                        // Current chart common candle index
                        val candleIndex = stockChart.data.getCandleSeries().binarySearch {
                            it.openInstant.compareTo(commonInstant)
                        }
                        // Iteration chart common candle index
                        val chartCandleIndex = chart.data.getCandleSeries().binarySearch {
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

        // Sync crosshair position across charts
        actualChart
            .crosshairMove()
            .onEach { mouseEventParams ->

                // If chart is not active (user hasn't interacted), skip sync
                if (lastActiveChart.value != stockChart) return@onEach

                if (mouseEventParams.logical == null) {
                    // Crosshair doesn't exist on current chart. Clear cross-hairs on other charts
                    charts.forEach { chart -> chart.actualChart.clearCrosshairPosition() }
                } else {

                    // Update crosshair on all other charts with same timeframe
                    charts
                        .filter { filterStockChart ->
                            // Select charts with same timeframe, ignore current chart
                            stockChart.params.timeframe == filterStockChart.params.timeframe &&
                                    filterStockChart != stockChart
                        }
                        .forEach { chart ->

                            // Set crosshair without price component
                            chart.actualChart.setCrosshairPosition(
                                price = 0,
                                horizontalPosition = mouseEventParams.time ?: return@forEach,
                                seriesApi = chart.candlestickPlotter.series,
                            )
                        }
                }
            }
            .launchIn(coroutineScope)

        return stockChart
    }

    private fun StockChart.newParams(params: StockChartParams) {

        val prevParams = params

        // Set StockChartData on StockChart
        setData(candleLoader.getStockChartData(params))

        // Release StockChartData if unused
        if (!charts.any { it.params == prevParams })
            candleLoader.releaseStockChartData(prevParams)
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
