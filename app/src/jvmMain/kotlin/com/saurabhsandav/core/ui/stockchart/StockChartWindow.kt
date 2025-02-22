package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.app.AppWindowState
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.webview.WebViewState
import com.saurabhsandav.core.ui.stockchart.ui.ChartsLayout
import com.saurabhsandav.core.ui.stockchart.ui.Tabs
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope

class StockChartWindow(
    parentScope: CoroutineScope,
    webViewStateProvider: (CoroutineScope) -> WebViewState,
    private val getStockChart: (ChartId) -> StockChart,
    private val onNewChart: (ChartPageState, ChartId?) -> ChartId,
    private val onSelectChart: (ChartId) -> Unit,
    private val onCloseChart: (ChartId) -> Unit,
    private val onChartActive: (ChartId) -> Unit,
) {

    internal val coroutineScope = parentScope.newChildScope()
    internal var appWindowState: AppWindowState? = null

    val pageState = ChartPageState(
        coroutineScope = coroutineScope,
        webViewState = webViewStateProvider(coroutineScope),
    )
    val tabChartIdMap = mutableMapOf<Int, ChartId>()
    val chartIds
        get() = tabChartIdMap.values

    internal var showTickerSelectionDialog by mutableStateOf(false)
    internal var showTimeframeSelectionDialog by mutableStateOf(false)

    private val queuedChartIds = mutableListOf<ChartId>()

    internal var layout: ChartsLayout by mutableStateOf(Tabs)

    internal val tabsState = StockChartTabsState(
        onNew = ::onNewTab,
        onSelect = ::onSelectTab,
        onClose = ::onCloseTab,
        title = { tabId -> getStockChart(getChartId(tabId)).title },
    )

    val selectedChartId by derivedStateOf {
        tabsState.tabIds.getOrNull(tabsState.selectedTabIndex)?.let(tabChartIdMap::get)
    }

    internal val chartInteraction = ChartInteraction(
        onChartHover = { selectedChartId?.let(onChartActive) },
        onChartSelected = { selectedChartId?.let(onChartActive) },
    )

    fun openChart(chartId: ChartId) {

        // Queue provided chart id
        queuedChartIds.add(chartId)

        // Open a new tab for queued chart id
        tabsState.newTab()
    }

    fun selectChart(chartId: ChartId) {

        val tabId = tabChartIdMap.filterValues { it == chartId }.keys.single()

        tabsState.selectTab(tabId)
    }

    fun toFront() {
        appWindowState?.toFront()
    }

    internal fun onSetLayout(layout: ChartsLayout) {

        this.layout = layout
    }

    private fun onNewTab(tabId: Int) {

        // Get queued chart id if available or request new chart and get id
        val chartId = queuedChartIds.removeFirstOrNull() ?: onNewChart(pageState, selectedChartId)

        // Save chart id
        tabChartIdMap[tabId] = chartId
    }

    private fun onSelectTab(tabId: Int) {

        val chartId = getChartId(tabId)

        // Show selected chart
        chartIds.forEach { iChartId ->

            when (iChartId) {
                chartId -> pageState.showChart(iChartId.value)
                else -> pageState.hideChart(iChartId.value)
            }
        }

        // Notify observer
        onSelectChart(chartId)
    }

    private fun onCloseTab(tabId: Int) {

        val chartId = getChartId(tabId)

        // Notify observer
        onCloseChart(chartId)

        // Remove saved chart id
        tabChartIdMap.remove(tabId)

        // Remove chart container from page
        pageState.removeChart(chartId.value)
    }

    private fun getChartId(tabId: Int): ChartId {
        return tabChartIdMap[tabId] ?: error("Tab($tabId doesn't exist)")
    }
}
