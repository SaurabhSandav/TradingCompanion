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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

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
    private var ignoreChartSyncUntil: Instant? = null

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

                // Sync visible range across charts
                actualChart.timeScale
                    .visibleLogicalRangeChange()
                    .filterNotNull()
                    .onEach { range ->

                        // Prevent infinite loop of current chart setting range of other charts which triggers
                        // the range change callbacks for those charts and so on
                        val ignoreChartSyncInstant = ignoreChartSyncUntil
                        if (ignoreChartSyncInstant != null && ignoreChartSyncInstant > Clock.System.now()) {
                            return@onEach
                        }

                        ignoreChartSyncUntil = Clock.System.now() + 500.milliseconds

                        // Update all other charts with same timeframe
                        windows.flatMap { it.charts.values }
                            .filter { filterStockChart ->
                                // Ignore chart if params not initialized
                                val currentChartTimeframe = stockChart.currentParams?.timeframe ?: return@filter false
                                // Select charts with same timeframe, ignore current chart
                                currentChartTimeframe == filterStockChart.currentParams?.timeframe &&
                                        filterStockChart != stockChart
                            }
                            .forEach {
                                it.actualChart.timeScale.setVisibleLogicalRange(range.from, range.to)
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

        windows += ChartWindow(
            charts = charts,
            tabsState = tabsState,
            pageState = pageState,
        )
    }

    fun closeWindow(chartWindow: ChartWindow): Boolean {

        if (windows.size == 1) return false

        windows.remove(chartWindow)

        return true
    }

    class ChartWindow(
        val charts: Map<Int, StockChart>,
        val tabsState: StockChartTabsState,
        val pageState: ChartPageState,
    ) {

        val selectedStockChart by derivedStateOf { charts.getValue(tabsState.tabs[tabsState.selectedTabIndex].id) }
    }
}
