package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.arrangement.PagedChartArrangement
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.ui.stockchart.data.CandleLoader
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.newChildScope
import com.saurabhsandav.lightweight_charts.options.ChartOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.CrosshairOptions.CrosshairMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.uuid.Uuid

class StockChartsState(
    parentScope: CoroutineScope,
    initialParams: StockChartParams?,
    loadConfig: LoadConfig,
    val marketDataProvider: MarketDataProvider,
    appPrefs: FlowSettings,
    private val chartPrefs: FlowSettings,
    private val webViewStateProvider: (CoroutineScope) -> WebViewState,
) {

    private val coroutineScope = parentScope.newChildScope()
    private val isDark = appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)
    private val lastActiveChartId = MutableStateFlow<ChartId?>(null)

    internal var isInitializedWithParams by mutableStateOf(initialParams != null)
    internal val windows = mutableStateListOf<StockChartWindow>()
    internal val idChartsMap = mutableMapOf<ChartId, StockChart>()
    val charts
        get() = idChartsMap.values

    private val candleLoader = CandleLoader(
        marketDataProvider = marketDataProvider,
        loadConfig = loadConfig,
        onCandlesLoaded = ::onCandlesLoaded,
    )

    init {

        val window = newWindow(null)
        if (initialParams != null) newChart(initialParams, window)

        // Setting dark mode according to settings
        coroutineScope.launch {
            isDark.collect { isDark ->
                charts.forEach { it.setDarkMode(isDark) }
            }
        }
    }

    fun newChart(
        params: StockChartParams,
        window: StockChartWindow?,
    ): StockChart {

        val lastActiveChartId = lastActiveChartId.value
        val window = when {
            window != null -> window
            // Get last active window based on last active chart
            lastActiveChartId != null -> windows.first { lastActiveChartId in it.chartIds }
            else -> windows.first()
        }

        val chartId = ChartId(Uuid.random().toString())

        // Create new StockChart
        val stockChart = newStockChart(chartId, window.pagedArrangement, params)

        idChartsMap[chartId] = stockChart

        // Add our new chart to the window
        window.openChart(chartId)

        return stockChart
    }

    fun bringToFront(stockChart: StockChart) {

        val window = windows.first { stockChart.chartId in it.chartIds }

        // Bring window to front
        window.toFront()

        // Select chart
        window.selectChart(stockChart.chartId)
    }

    fun reset() = coroutineScope.launchUnit {
        candleLoader.reset()
    }

    internal fun onInitializeChart(
        window: StockChartWindow,
        ticker: String,
        timeframe: Timeframe,
    ) {

        onOpenInNewTab(window, ticker, timeframe)

        isInitializedWithParams = true
    }

    internal fun newWindow(launchedFrom: StockChartWindow?): StockChartWindow {

        val window = StockChartWindow(
            parentScope = coroutineScope,
            webViewStateProvider = webViewStateProvider,
            getStockChart = ::getStockChart,
            onNewChart = { arrangement, selectedChartId ->

                val fromStockChart = (selectedChartId ?: lastActiveChartId.value)
                    ?.let(::getStockChart)
                    ?: error("No chart params to open")

                val chartId = ChartId(Uuid.random().toString())

                val stockChart = newStockChart(
                    chartId = chartId,
                    arrangement = arrangement,
                    params = fromStockChart.params,
                    initialVisibleRange = fromStockChart.visibleRange,
                )

                idChartsMap[chartId] = stockChart

                chartId
            },
            onSelectChart = { chartId ->

                // Set selected chart as lastActiveChart
                lastActiveChartId.value = chartId
            },
            onCloseChart = { chartId ->

                val stockChart = getStockChart(chartId)

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
            .onEach { actualChart ->
                lastActiveChartId.value = charts.first { it.actualChart == actualChart }.chartId
            }
            .launchIn(window.coroutineScope)

        // Create an initial chart in the new window
        val launchedFromStockChart = launchedFrom?.selectedChartId?.let(::getStockChart)
        if (launchedFromStockChart != null) {

            newChart(
                params = launchedFromStockChart.params,
                window = window,
            )
        }

        return window
    }

    internal fun getStockChart(chartId: ChartId): StockChart {
        return idChartsMap[chartId] ?: error("Chart(${chartId.value} doesn't exist)")
    }

    internal fun closeWindow(window: StockChartWindow): Boolean {

        if (windows.size == 1) return false

        windows.remove(window)

        window.coroutineScope.cancel()

        return true
    }

    internal fun onChangeTicker(window: StockChartWindow, ticker: String) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        // New chart params
        stockChart.newParams(stockChart.params.copy(ticker = ticker))
    }

    internal fun onChangeTimeframe(window: StockChartWindow, timeframe: Timeframe) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        // New chart params
        stockChart.newParams(stockChart.params.copy(timeframe = timeframe))
    }

    internal fun onOpenInNewTab(window: StockChartWindow, ticker: String, timeframe: Timeframe) {
        newChart(StockChartParams(ticker, timeframe), window)
    }

    internal fun goToDateTime(
        window: StockChartWindow,
        dateTime: LocalDateTime?,
    ) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        // Navigate to datetime
        coroutineScope.launch {
            stockChart.navigateTo(instant = dateTime?.toInstant(TimeZone.currentSystemDefault()))
        }
    }

    private fun newStockChart(
        chartId: ChartId,
        arrangement: PagedChartArrangement,
        params: StockChartParams,
        initialVisibleRange: ClosedRange<Float>? = null,
    ): StockChart {

        // New chart
        val actualChart = arrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        // New StockChart
        val stockChart = StockChart(
            parentScope = coroutineScope,
            chartId = chartId,
            prefs = chartPrefs,
            marketDataProvider = marketDataProvider,
            candleLoader = candleLoader,
            actualChart = actualChart,
            initialData = candleLoader.getStockChartData(params),
            initialVisibleRange = initialVisibleRange,
        )

        // Initial theme
        stockChart.setDarkMode(isDark.value)

        // Sync visible range across charts
        actualChart.timeScale
            .visibleLogicalRangeChange()
            .filterNotNull()
            .onEach { logicalRange ->

                // If chart is not active (user hasn't interacted), skip sync
                if (lastActiveChartId.value != stockChart.chartId) return@onEach

                // Current instant range. If not populated, skip sync
                val instantRange = stockChart.data.candleSeries.instantRange.value ?: return@onEach

                // Update all other charts with same timeframe
                charts
                    .filter { filterStockChart ->
                        // Select charts with same timeframe, ignore current chart
                        stockChart.params.timeframe == filterStockChart.params.timeframe &&
                                filterStockChart != stockChart
                    }
                    .forEach { chart ->

                        // Chart instant range. If not populated, skip sync
                        val chartInstantRange = chart.data.candleSeries.instantRange.value ?: return@forEach

                        // Intersection range of current chart and iteration chart
                        // Skip sync if there is no overlap in instant ranges
                        val intersection = instantRange.intersect(chartInstantRange) ?: return@forEach

                        // Pick a common candle instant to use for calculating a sync offset
                        val commonInstant = intersection.endInclusive

                        // Current chart common candle index
                        val candleIndex = stockChart.data.candleSeries.binarySearch {
                            it.openInstant.compareTo(commonInstant)
                        }
                        // Iteration chart common candle index
                        val chartCandleIndex = chart.data.candleSeries.binarySearch {
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
            .launchIn(stockChart.coroutineScope)

        // Sync crosshair position across charts
        actualChart
            .crosshairMove()
            .onEach { mouseEventParams ->

                // If chart is not active (user hasn't interacted), skip sync
                if (lastActiveChartId.value != stockChart.chartId) return@onEach

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
                                price = 0.0,
                                horizontalPosition = mouseEventParams.time ?: return@forEach,
                                seriesApi = chart.candlestickPlotter.series,
                            )
                        }
                }
            }
            .launchIn(stockChart.coroutineScope)

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

    private fun onCandlesLoaded(params: StockChartParams) {
        charts
            .filter { stockChart -> stockChart.params == params }
            .forEach { stockChart -> stockChart.setDataToChart() }
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

@JvmInline
value class ChartId(val value: String)
