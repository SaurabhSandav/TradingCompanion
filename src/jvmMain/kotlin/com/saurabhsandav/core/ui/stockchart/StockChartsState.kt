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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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
                newStockChart(arrangement, currentStockChart ?: fromStockChart)
            },
            onSelectChart = { stockChart ->

                // Set selected chart as lastActiveChart
                lastActiveChart.value = stockChart
            },
            onCloseChart = { stockChart ->

                // Destroy chart
                stockChart.destroy()

                // Notify observer
                onCloseChart(stockChart)
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

    fun openNewTab() {

        val lastActiveChart = checkNotNull(lastActiveChart.value) { "No last active chart" }

        // Find window with the last active chart, and open a new tab.
        windows.first { lastActiveChart in it.charts }.tabsState.newTab()
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
        fromStockChart: StockChart?,
    ): StockChart {

        // New chart
        val actualChart = arrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        // New StockChart
        val stockChart = StockChart(
            appModule = appModule,
            actualChart = actualChart,
            onLegendUpdate = { arrangement.setLegend(actualChart, it) },
        )

        // Initial theme
        stockChart.setDarkMode(isDark.value)

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
                charts
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

        // Notify observer
        onNewChart(stockChart, fromStockChart)

        return stockChart
    }
}
