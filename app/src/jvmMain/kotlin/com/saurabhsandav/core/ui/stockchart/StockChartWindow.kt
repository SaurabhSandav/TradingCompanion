package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
    private val onCreateChart: (ChartPageState, ChartId?) -> ChartId,
    private val onChartSelected: (ChartId) -> Unit,
    private val onDestroyChart: (ChartId) -> Unit,
    private val onChartActive: (ChartId) -> Unit,
) {

    internal val coroutineScope = parentScope.newChildScope()
    internal var appWindowState: AppWindowState? = null

    val pageState = ChartPageState(
        coroutineScope = coroutineScope,
        webViewState = webViewStateProvider(coroutineScope),
    )
    val chartIds = mutableStateListOf<ChartId>()
    var selectedChartId: ChartId? by mutableStateOf(null)
    val selectedChartIndex by derivedStateOf { chartIds.indexOf(selectedChartId) }

    private val queuedChartIds = mutableListOf<ChartId>()

    internal var layout: ChartsLayout by mutableStateOf(Tabs)

    internal var showTickerSelectionDialog by mutableStateOf(false)
    internal var showTimeframeSelectionDialog by mutableStateOf(false)

    internal val chartInteraction = ChartInteraction(
        onChartHover = { selectedChartId?.let(onChartActive) },
        onChartSelected = { selectedChartId?.let(onChartActive) },
    )

    fun openChart(chartId: ChartId) {

        // Queue provided chart id
        queuedChartIds.add(chartId)

        onNewChart()
    }

    fun selectChart(chartId: ChartId) {

        selectedChartId = chartId

        onSelectChart(chartId)
    }

    fun toFront() {
        appWindowState?.toFront()
    }

    internal fun getChartTitle(chartId: ChartId): String {
        return getStockChart(chartId).title
    }

    internal fun onSetLayout(layout: ChartsLayout) {

        this.layout = layout
    }

    internal fun onNewChart() {

        // Get queued chart id if available or request new chart and get id
        val chartId = queuedChartIds.removeFirstOrNull() ?: onCreateChart(pageState, selectedChartId)

        // Add ChartId after currently selected ChartId
        chartIds.add(selectedChartIndex + 1, chartId)

        onSelectChart(chartId)
    }

    internal fun onSelectChart(chartId: ChartId) {

        selectedChartId = chartId

        // Show selected chart
        chartIds.forEach { iChartId ->

            when (iChartId) {
                chartId -> pageState.showChart(iChartId.value)
                else -> pageState.hideChart(iChartId.value)
            }
        }

        onChartSelected(chartId)
    }

    internal fun onCloseChart(chartId: ChartId) {

        if (chartIds.size <= 1) return

        val closeChartIndex = chartIds.indexOf(chartId)

        if (closeChartIndex == selectedChartIndex) {

            // Try selecting next chart, or if current chart is last, select previous chart
            val nextSelectedChartId = when {
                selectedChartIndex == chartIds.lastIndex -> chartIds[selectedChartIndex - 1]
                else -> chartIds[selectedChartIndex + 1]
            }

            // Select another chart
            onSelectChart(nextSelectedChartId)
        }

        onDestroyChart(chartId)

        // Remove saved chart id
        chartIds.remove(chartId)

        // Remove chart container from page
        pageState.removeChart(chartId.value)
    }

    internal fun onCloseCurrentChart() {
        onCloseChart(selectedChartId!!)
    }

    internal fun onSelectNextChart() {

        val nextSelectionIndex = when {
            selectedChartIndex == chartIds.lastIndex -> 0
            else -> selectedChartIndex + 1
        }

        onSelectChart(chartIds[nextSelectionIndex])
    }

    internal fun onSelectPreviousChart() {

        val previousSelectionIndex = when {
            selectedChartIndex == 0 -> chartIds.lastIndex
            else -> selectedChartIndex - 1
        }

        onSelectChart(chartIds[previousSelectionIndex])
    }

    internal fun onMoveChartBackward() {

        if (selectedChartIndex == 0) return

        val moveTo = selectedChartIndex - 1
        val chartId = chartIds.removeAt(selectedChartIndex)

        chartIds.add(moveTo, chartId)
    }

    internal fun onMoveChartForward() {

        if (selectedChartIndex == chartIds.lastIndex) return

        val moveTo = selectedChartIndex + 1
        val chartId = chartIds.removeAt(selectedChartIndex)

        chartIds.add(moveTo, chartId)
    }
}
