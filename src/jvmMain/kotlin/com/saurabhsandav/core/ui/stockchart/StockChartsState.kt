package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val pagedArrangement = ChartArrangement.paged()
    private val charts = mutableMapOf<Int, StockChart>()
    private val isDark = appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val tabsState: StockChartTabsState
    val pageState: ChartPageState = ChartPageState(coroutineScope, pagedArrangement)
    var currentStockChart: StockChart? by mutableStateOf(null)

    init {

        tabsState = StockChartTabsState(
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
                onNewChart(stockChart, charts[prevTabId])

                // Initial theme
                stockChart.setDarkMode(isDark.value)

                // Cache chart
                charts[tabId] = stockChart

                // Connect chart to web page
                pageState.connect(chart = actualChart)
            },
            onSelect = { tabId ->

                val stockChart = charts.getValue(tabId)

                // Update current chart
                currentStockChart = stockChart

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

        // Setting dark mode according to settings
        coroutineScope.launch {
            isDark.collect { isDark ->
                charts.values.forEach { it.setDarkMode(isDark) }
            }
        }
    }

    fun changeTicker(ticker: String) {

        val stockChart = charts.getValue(tabsState.selectedTabIndex)

        onChangeTicker(stockChart, ticker)
    }

    fun changeTimeframe(timeframe: Timeframe) {

        val stockChart = charts.getValue(tabsState.selectedTabIndex)

        onChangeTimeframe(stockChart, timeframe)
    }
}
