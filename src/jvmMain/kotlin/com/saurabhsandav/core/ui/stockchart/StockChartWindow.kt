package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.common.app.AppWindowState
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.PagedChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import kotlinx.coroutines.MainScope

internal class StockChartWindow(
    private val onNewChart: (
        pagedArrangement: PagedChartArrangement,
        currentStockChart: StockChart?,
    ) -> StockChart,
    private val onSelectChart: (StockChart) -> Unit,
    private val onCloseChart: (StockChart) -> Unit,
) {

    val coroutineScope = MainScope()
    lateinit var appWindowState: AppWindowState

    val pagedArrangement = ChartArrangement.paged()
    val pageState = ChartPageState(coroutineScope, pagedArrangement)
    val tabCharts = mutableMapOf<Int, StockChart>()
    val charts
        get() = tabCharts.values.toList()

    private var currentStockChart: StockChart? = null
    private val queuedCharts = mutableListOf<StockChart>()

    val tabsState = StockChartTabsState(
        onNew = ::onNewTab,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        title = { tabId -> tabCharts.getValue(tabId).title }
    )

    val selectedStockChart by derivedStateOf { tabCharts.getValue(tabsState.tabIds[tabsState.selectedTabIndex]) }

    fun openChart(stockChart: StockChart) {

        // Queue provided chart
        queuedCharts.add(stockChart)

        // Open a new tab for queued chart
        tabsState.newTab()
    }

    private fun onNewTab(tabId: Int) {

        // Get queued chart if available or request new chart
        val stockChart = queuedCharts.removeFirstOrNull() ?: onNewChart(pagedArrangement, currentStockChart)

        // Save chart
        tabCharts[tabId] = stockChart

        // Connect chart to web page
        pageState.connect(chart = stockChart.actualChart)
    }

    private fun onSelectTab(tabId: Int) {

        val stockChart = tabCharts.getValue(tabId)

        // Update current chart
        currentStockChart = stockChart

        // Show selected chart
        pagedArrangement.showChart(stockChart.actualChart)

        // Notify observer
        onSelectChart(stockChart)
    }

    private fun onCloseTab(tabId: Int) {

        val stockChart = tabCharts.getValue(tabId)

        // Remove saved chart
        tabCharts.remove(tabId)

        // Remove chart page
        pagedArrangement.removeChart(stockChart.actualChart)

        // Disconnect chart from web page
        pageState.disconnect(stockChart.actualChart)

        // Notify observer
        onCloseChart(stockChart)
    }
}
