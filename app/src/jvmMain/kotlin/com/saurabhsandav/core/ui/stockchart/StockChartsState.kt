package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.data.StockChartData
import com.saurabhsandav.core.ui.stockchart.ui.Tabs
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.newChildScope
import com.saurabhsandav.lightweightcharts.options.ChartOptions
import com.saurabhsandav.lightweightcharts.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.lightweightcharts.options.ChartOptions.CrosshairOptions.CrosshairMode
import com.saurabhsandav.lightweightcharts.options.TimeScaleOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.uuid.Uuid

class StockChartsState(
    parentScope: CoroutineScope,
    initialParams: StockChartParams?,
    private val loadConfig: LoadConfig,
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

    private val syncManager = StockChartsSyncManager(
        charts = { charts },
        lastActiveChartId = { lastActiveChartId.value },
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

        val isCalledExternally = window == null

        val lastActiveChartId = lastActiveChartId.value
        val window = when {
            window != null -> window
            // Get last active window based on last active chart
            lastActiveChartId != null -> windows.first { lastActiveChartId in it.chartIds }
            else -> windows.first()
        }

        // Charts opened from outside should be opened in Tabs layout
        if (isCalledExternally && window.layout != Tabs) window.onSetLayout(Tabs)

        val chartId = ChartId(Uuid.random().toString())

        // Create new StockChart
        val stockChart = newStockChart(chartId, window.pageState, params)

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

    internal fun onInitializeChart(
        window: StockChartWindow,
        ticker: String,
        timeframe: Timeframe,
    ) {

        onOpenInCurrentWindow(window, ticker, timeframe)

        isInitializedWithParams = true
    }

    internal fun newWindow(launchedFrom: StockChartWindow?): StockChartWindow {

        val window = StockChartWindow(
            parentScope = coroutineScope,
            webViewStateProvider = webViewStateProvider,
            getStockChart = ::getStockChart,
            onCreateChart = { pageState, selectedChartId ->

                val fromStockChart = (selectedChartId ?: lastActiveChartId.value)
                    ?.let(::getStockChart)
                    ?: error("No chart params to open")

                val chartId = ChartId(Uuid.random().toString())

                val stockChart = newStockChart(
                    chartId = chartId,
                    pageState = pageState,
                    params = fromStockChart.params,
                    initialVisibleRange = fromStockChart.visibleRange,
                )

                idChartsMap[chartId] = stockChart

                chartId
            },
            onChartSelected = { chartId ->

                // Set selected chart as lastActiveChart
                lastActiveChartId.value = chartId

                syncManager.onChartActive(getStockChart(chartId))
            },
            onDestroyChart = { chartId ->

                val stockChart = getStockChart(chartId)

                // Destroy chart
                stockChart.destroy()
            },
            onChartActive = { chartId ->

                // Update last active chart
                lastActiveChartId.value = chartId

                syncManager.onChartActive(getStockChart(chartId))
            },
        )

        windows += window

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

    internal fun getStockChartOrNull(
        window: StockChartWindow,
        chartIndex: Int,
    ): StockChart? {
        val chartId = window.getChartId(chartIndex) ?: return null
        return getStockChart(chartId)
    }

    internal fun closeWindow(window: StockChartWindow): Boolean {

        if (windows.size == 1) return false

        windows.remove(window)

        window.coroutineScope.cancel()

        return true
    }

    internal fun onChangeTicker(
        window: StockChartWindow,
        ticker: String,
    ) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        // New chart params
        stockChart.newParams(stockChart.params.copy(ticker = ticker))
    }

    internal fun onChangeTimeframe(
        window: StockChartWindow,
        timeframe: Timeframe,
    ) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        // New chart params
        stockChart.newParams(stockChart.params.copy(timeframe = timeframe))
    }

    internal fun onOpenInCurrentWindow(
        window: StockChartWindow,
        ticker: String? = null,
        timeframe: Timeframe? = null,
    ) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return
        val ticker = ticker ?: stockChart.params.ticker
        val timeframe = timeframe ?: stockChart.params.timeframe

        newChart(StockChartParams(ticker, timeframe), window)
    }

    internal fun onOpenInNewWindow(
        window: StockChartWindow,
        ticker: String? = null,
        timeframe: Timeframe? = null,
    ) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return
        val ticker = ticker ?: stockChart.params.ticker
        val timeframe = timeframe ?: stockChart.params.timeframe

        newChart(StockChartParams(ticker, timeframe), newWindow(null))
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

    internal fun goToLatest(window: StockChartWindow) {

        val stockChart = window.selectedChartId?.let(::getStockChart) ?: return

        coroutineScope.launch {
            stockChart.navigateToLatest()
        }
    }

    private fun newStockChart(
        chartId: ChartId,
        pageState: ChartPageState,
        params: StockChartParams,
        initialVisibleRange: ClosedRange<Float>? = null,
    ): StockChart {

        // New chart
        val actualChart = pageState.addChart(
            id = chartId.value,
            options = ChartOptions(
                crosshair = CrosshairOptions(mode = CrosshairMode.Normal),
                timeScale = TimeScaleOptions(lockVisibleTimeRangeOnResize = true),
            ),
        )

        // New StockChart
        val stockChart = StockChart(
            parentScope = coroutineScope,
            chartId = chartId,
            prefs = chartPrefs,
            marketDataProvider = marketDataProvider,
            actualChart = actualChart,
            syncManager = syncManager,
            initialParams = params,
            buildStockChartData = { params, loadedPages ->

                StockChartData(
                    source = marketDataProvider.buildCandleSource(params),
                    loadConfig = loadConfig,
                    loadedPages = loadedPages,
                    onCandlesLoaded = {
                        val stockChart = getStockChart(chartId)
                        syncManager.onCandlesLoaded(stockChart)
                        stockChart.plotterManager.setData()
                    },
                )
            },
            initialVisibleRange = initialVisibleRange,
            onShowTickerSelector = {
                val window = windows.first { window -> chartId in window.chartIds }
                window.showTickerSelectionDialog = true
            },
            onShowTimeframeSelector = {
                val window = windows.first { window -> chartId in window.chartIds }
                window.showTimeframeSelectionDialog = true
            },
        )

        // Initial theme
        stockChart.setDarkMode(isDark.value)

        return stockChart
    }
}

@JvmInline
value class ChartId(
    val value: String,
)
