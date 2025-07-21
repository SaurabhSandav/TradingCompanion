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
import com.saurabhsandav.core.ui.stockchart.ui.PanesLayout
import com.saurabhsandav.core.ui.stockchart.ui.Tabs
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope

class StockChartWindow(
    parentScope: CoroutineScope,
    webViewStateFactory: WebViewState.Factory,
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
        webViewState = webViewStateFactory.create(coroutineScope),
    )
    val chartIds = mutableStateListOf<ChartId>()
    var selectedChartId: ChartId? by mutableStateOf(null)
    val selectedChartIndex by derivedStateOf { chartIds.indexOf(selectedChartId) }

    private val queuedChartIds = mutableListOf<ChartId>()

    internal var layout: ChartsLayout by mutableStateOf(Tabs)
    private var queuedLayout: ChartsLayout? = null

    internal var showSymbolSelectionDialog by mutableStateOf(false)
    internal var showTimeframeSelectionDialog by mutableStateOf(false)
    internal var showLayoutChangeConfirmationDialog by mutableStateOf(false)
    internal var canOpenNewChart by mutableStateOf(true)

    internal val chartInteraction = ChartInteraction(
        layout = { layout },
        onChartHover = { chartIndex ->
            val chartId = getChartId(chartIndex) ?: return@ChartInteraction
            onChartActive(chartId)
        },
        onChartSelected = { chartIndex ->
            val chartId = getChartId(chartIndex) ?: return@ChartInteraction
            onSelectChart(chartId)
        },
    )

    fun openChart(chartId: ChartId) {

        if (layout is PanesLayout && layout.rects.size == chartIds.size) return

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

    internal fun getChartId(chartIndex: Int): ChartId? {
        return when (layout) {
            is Tabs -> selectedChartId
            is PanesLayout -> chartIds.getOrNull(chartIndex)
        }
    }

    internal fun getChartTitle(chartId: ChartId): String {
        return getStockChart(chartId).title
    }

    internal fun onSetLayout(layout: ChartsLayout) {

        val layoutChanged = when (layout) {
            is Tabs -> setTabsLayout(layout)
            is PanesLayout -> setPanesLayout(layout)
        }

        if (layoutChanged) {
            this.layout = layout
        }
    }

    private fun setTabsLayout(layout: Tabs): Boolean {

        if (this.layout is Tabs) return false

        val rect = layout.rects.single()

        chartIds.forEach { chartId ->

            setChartLayout(chartId, layout, rect)

            when (chartId) {
                selectedChartId -> pageState.showChart(chartId.value)
                else -> pageState.hideChart(chartId.value)
            }
        }

        canOpenNewChart = true

        return true
    }

    private fun setPanesLayout(layout: PanesLayout): Boolean {

        // If current layout contains more charts than can fit into new layout, ask confirmation for closing extra charts.
        if (chartIds.size > layout.rects.size) {
            queuedLayout = layout
            showLayoutChangeConfirmationDialog = true
            return false
        }

        // Set charts as panes by order in chartIds list
        layout.rects.forEachIndexed { chartIndex, rect ->

            val chartId = chartIds.getOrNull(chartIndex) ?: return@forEachIndexed

            setChartLayout(chartId, layout, rect)

            pageState.showChart(chartId.value)
        }

        canOpenNewChart = chartIds.size < layout.rects.size

        return true
    }

    private fun setChartLayout(
        chartId: ChartId,
        layout: ChartsLayout,
        rect: ChartsLayout.GridRect,
    ) {

        val gridColumns = layout.gridSize.columns.toFloat()
        val gridRows = layout.gridSize.rows.toFloat()

        val left = (rect.left / gridColumns) * 100
        val top = (rect.top / gridRows) * 100
        val width = (rect.columns / gridColumns) * 100
        val height = (rect.rows / gridRows) * 100

        pageState.setChartLayout(
            id = chartId.value,
            left = "$left%",
            top = "$top%",
            width = "$width%",
            height = "$height%",
        )
    }

    internal fun onLayoutChangeConfirmed() {

        showLayoutChangeConfirmationDialog = false

        val layout = queuedLayout!!
        queuedLayout = null

        // Close extra charts
        chartIds
            .drop(layout.rects.size)
            .forEach(::onCloseChart)

        onSetLayout(layout)
    }

    internal fun onLayoutChangeCancelled() {
        showLayoutChangeConfirmationDialog = false
        queuedLayout = null
    }

    internal fun onNewChart() {

        if (layout is PanesLayout && layout.rects.size == chartIds.size) return

        // Get queued chart id if available or request new chart and get id
        val chartId = queuedChartIds.removeFirstOrNull() ?: onCreateChart(pageState, selectedChartId)

        // Add ChartId after currently selected ChartId
        chartIds.add(selectedChartIndex + 1, chartId)

        onSelectChart(chartId)

        onSetLayout(layout)
    }

    internal fun onSelectChart(chartId: ChartId) {

        selectedChartId = chartId

        if (layout is Tabs) {

            // Show selected chart
            chartIds.forEach { iChartId ->

                when (iChartId) {
                    chartId -> pageState.showChart(iChartId.value)
                    else -> pageState.hideChart(iChartId.value)
                }
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

        onSetLayout(layout)
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
