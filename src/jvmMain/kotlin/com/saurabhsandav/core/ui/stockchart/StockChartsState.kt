package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDateTime

@Stable
internal class StockChartsState(
    val onNewChart: (
        newStockChart: StockChart,
        prevStockChart: StockChart?,
    ) -> Unit,
    val onCloseChart: (StockChart) -> Unit,
    val onChangeTicker: (StockChart, String) -> Unit,
    val onChangeTimeframe: (StockChart, Timeframe) -> Unit,
    private val appModule: AppModule,
    val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val coroutineScope = MainScope()
    private val isDark = appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)
    private val lastActiveChart = MutableStateFlow<StockChart?>(null)

    val windows = mutableStateListOf<ChartWindow>()

    init {

        newWindow(null)

        // Setting dark mode according to settings
        coroutineScope.launch {
            isDark.collect { isDark ->
                windows.flatMap { it.charts.values }.forEach { it.setDarkMode(isDark) }
            }
        }
    }

    fun goToDateTime(stockChart: StockChart, dateTime: LocalDateTime?) = coroutineScope.launchUnit {

        // Load data if date specified
        if (dateTime != null) {
            windows.flatMap { it.charts.values }
                .map { it.loadDateTime(dateTime) }
                .joinAll()
        }

        // Navigate to datetime, other charts should be synced to same datetime
        stockChart.goToDateTime(dateTime)
    }

    fun newWindow(fromStockChart: StockChart?) {

        val pagedArrangement = ChartArrangement.paged()
        val charts = mutableMapOf<Int, StockChart>()
        val pageState = ChartPageState(coroutineScope, pagedArrangement)
        val currentStockChart = mutableStateOf<StockChart?>(null)

        val tabsState = StockChartTabsState(
            onNew = { tabId, prevTabId, updateTitle ->

                // New chart
                val actualChart = pagedArrangement.newChart(
                    options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
                )

                // New StockChart
                val stockChart = StockChart(
                    appModule = appModule,
                    actualChart = actualChart,
                    onLegendUpdate = { pagedArrangement.setLegend(actualChart, it) },
                    onTitleUpdate = updateTitle,
                )

                // Notify observer
                onNewChart(stockChart, charts[prevTabId] ?: fromStockChart)

                // Initial theme
                stockChart.setDarkMode(isDark.value)

                // Cache chart
                charts[tabId] = stockChart

                // Connect chart to web page
                pageState.connect(chart = actualChart)

                // Set initial lastActiveChart
                lastActiveChart.value = stockChart

                // Sync visible range across charts
                actualChart.timeScale
                    .visibleLogicalRangeChange()
                    .filterNotNull()
                    .onEach { range ->

                        // If chart is not active (user hasn't interacted), skip sync
                        if (lastActiveChart.value != stockChart) return@onEach

                        // Nothing to sync if chart does not have a candle source or sync key
                        val currentChartSyncKey = stockChart.source?.syncKey ?: return@onEach

                        // Previously sync was not accurate if no. of candles across charts was not the same.
                        // Offsets from the last candle should provide a more accurate way to sync charts in such cases.
                        // Note: This method does not work if the last candle of all (to-sync) charts is not at the
                        // same Instant. Should be fixed once live candles are implemented.
                        val startOffset = stockChart.source!!.candleSeries.size - range.from
                        val endOffset = stockChart.source!!.candleSeries.size - range.to

                        // Update all other charts with same timeframe
                        windows.flatMap { it.charts.values }
                            .filter { filterStockChart ->
                                // Select charts with same sync key, ignore current chart
                                currentChartSyncKey == filterStockChart.source?.syncKey &&
                                        filterStockChart != stockChart
                            }
                            .forEach {
                                it.actualChart.timeScale.setVisibleLogicalRange(
                                    from = it.source!!.candleSeries.size - startOffset,
                                    to = it.source!!.candleSeries.size - endOffset,
                                )
                            }
                    }
                    .launchIn(coroutineScope)
            },
            onSelect = { tabId ->

                val stockChart = charts.getValue(tabId)

                // Update current chart
                currentStockChart.value = stockChart

                // Show selected chart
                pagedArrangement.showChart(stockChart.actualChart)

                // Set selected chart as lastActiveChart
                lastActiveChart.value = stockChart
            },
            onClose = { tabId ->

                val stockChart = charts.getValue(tabId)

                // Remove chart from cache
                charts.remove(tabId)

                // Remove chart page
                pagedArrangement.removeChart(stockChart.actualChart)

                // Disconnect chart from web page
                pageState.disconnect(stockChart.actualChart)

                // Destroy chart
                stockChart.destroy()

                // Notify observer
                onCloseChart(stockChart)
            },
        )

        val coroutineScope = MainScope()

        windows += ChartWindow(
            coroutineScope = coroutineScope,
            charts = charts,
            tabsState = tabsState,
            pageState = pageState,
        )

        // Update last active chart
        pagedArrangement.lastActiveChart
            .onEach { actualChart -> lastActiveChart.value = charts.values.first { it.actualChart == actualChart } }
            .launchIn(coroutineScope)
    }

    fun closeWindow(chartWindow: ChartWindow): Boolean {

        if (windows.size == 1) return false

        windows.remove(chartWindow)

        chartWindow.coroutineScope.cancel()

        return true
    }

    class ChartWindow(
        val coroutineScope: CoroutineScope,
        val charts: Map<Int, StockChart>,
        val tabsState: StockChartTabsState,
        val pageState: ChartPageState,
    ) {

        val selectedStockChart by derivedStateOf { charts.getValue(tabsState.tabs[tabsState.selectedTabIndex].id) }
    }
}
