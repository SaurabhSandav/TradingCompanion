package com.saurabhsandav.core.ui.stockchart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.stockchart.ui.ChartOverlay
import com.saurabhsandav.core.ui.stockchart.ui.ChartsLayout
import com.saurabhsandav.core.ui.stockchart.ui.Legend
import com.saurabhsandav.core.ui.stockchart.ui.NewChartForm
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTabRow
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTopBar
import com.saurabhsandav.core.ui.stockchart.ui.StockChartWindowDecorator
import com.saurabhsandav.core.ui.stockchart.ui.Tabs
import com.saurabhsandav.core.ui.stockchart.ui.TimeframeSelectionDialog
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionDialog
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
import kotlinx.datetime.LocalDateTime

@Composable
fun StockCharts(
    onCloseRequest: () -> Unit,
    state: StockChartsState,
    windowTitle: String,
    decorationType: StockChartDecorationType,
    windowDecorator: StockChartWindowDecorator = StockChartWindowDecorator.Default,
    customShortcuts: ((KeyEvent) -> Boolean)? = null,
) {

    state.windows.forEach { chartWindow ->

        key(chartWindow) {

            Window(
                onCloseRequest = { if (!state.closeWindow(chartWindow)) onCloseRequest() },
                state = state,
                chartWindow = chartWindow,
                windowTitle = windowTitle,
                decorationType = decorationType,
                windowDecorator = windowDecorator,
                customShortcuts = customShortcuts,
            )
        }
    }
}

@Composable
private fun Window(
    onCloseRequest: () -> Unit,
    state: StockChartsState,
    chartWindow: StockChartWindow,
    windowTitle: String,
    decorationType: StockChartDecorationType,
    windowDecorator: StockChartWindowDecorator = StockChartWindowDecorator.Default,
    customShortcuts: ((KeyEvent) -> Boolean)? = null,
) {

    var initialFilterQuery by state { "" }

    AppWindow(
        onCloseRequest = onCloseRequest,
        title = windowTitle,
        state = rememberAppWindowState(preferredPlacement = WindowPlacement.Maximized),
    ) {

        chartWindow.appWindowState = LocalAppWindowState.current

        windowDecorator.Decoration {

            val tickers by state.marketDataProvider.symbols().collectAsState()
            val timeframes by state.marketDataProvider.timeframes().collectAsState()

            Box(
                modifier = Modifier.onPreviewKeyEvent { keyEvent ->

                    when {
                        chartKeyboardShortcuts(chartWindow, keyEvent) { initialFilterQuery = it.toString() } -> true
                        else -> customShortcuts?.invoke(keyEvent) == true
                    }
                },
            ) {

                val selectedStockChart = remember(chartWindow.selectedChartId) {
                    chartWindow.selectedChartId?.let(state::getStockChart)
                }
                val syncPrefs by state.syncPrefs.collectAsState()

                when {
                    !state.isInitializedWithParams -> NewChartForm(
                        tickers = tickers,
                        timeframes = timeframes,
                        onInitializeChart = { ticker, timeframe ->
                            state.onInitializeChart(chartWindow, ticker, timeframe)
                        },
                    )

                    selectedStockChart == null -> CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    )

                    else -> StockChartScreen(
                        chartWindow = chartWindow,
                        selectedStockChart = selectedStockChart,
                        getStockChartOrNull = { chartIndex -> state.getStockChartOrNull(chartWindow, chartIndex) },
                        decorationType = decorationType,
                        onOpenTickerSelection = { chartWindow.showTickerSelectionDialog = true },
                        onOpenTimeframeSelection = { chartWindow.showTimeframeSelectionDialog = true },
                        onGoToDateTime = { dateTime -> state.goToDateTime(chartWindow, dateTime) },
                        onGoToLatest = { state.goToLatest(chartWindow) },
                        layout = chartWindow.layout,
                        onSetLayout = chartWindow::onSetLayout,
                        syncPrefs = syncPrefs,
                        onToggleSyncCrosshair = state::onToggleSyncCrosshair,
                        onToggleSyncTime = state::onToggleSyncTime,
                        onToggleSyncDateRange = state::onToggleSyncDateRange,
                        onNewWindow = { state.newWindow(chartWindow) },
                    )
                }
            }

            if (chartWindow.showTickerSelectionDialog) {

                TickerSelectionDialog(
                    onDismissRequest = {
                        chartWindow.showTickerSelectionDialog = false
                        initialFilterQuery = ""
                    },
                    tickers = tickers,
                    onSelect = { ticker -> state.onChangeTicker(chartWindow, ticker) },
                    type = TickerSelectionType.Chart(
                        onOpenInCurrentWindow = when {
                            chartWindow.canOpenNewChart -> { ticker ->
                                state.onOpenInCurrentWindow(chartWindow, ticker, null)
                            }

                            else -> null
                        },
                        onOpenInNewWindow = { ticker -> state.onOpenInNewWindow(chartWindow, ticker, null) },
                    ),
                    initialFilterQuery = initialFilterQuery,
                )
            }

            if (chartWindow.showTimeframeSelectionDialog) {

                TimeframeSelectionDialog(
                    onDismissRequest = {
                        chartWindow.showTimeframeSelectionDialog = false
                        initialFilterQuery = ""
                    },
                    timeframes = timeframes,
                    initialFilterQuery = initialFilterQuery,
                    onSelect = { timeframe -> state.onChangeTimeframe(chartWindow, timeframe) },
                    onOpenInCurrentWindow = when {
                        chartWindow.canOpenNewChart -> { timeframe ->
                            state.onOpenInCurrentWindow(chartWindow, null, timeframe)
                        }

                        else -> null
                    },
                    onOpenInNewWindow = { timeframe -> state.onOpenInNewWindow(chartWindow, null, timeframe) },
                )
            }

            if (chartWindow.showLayoutChangeConfirmationDialog) {

                ConfirmationDialog(
                    text = "Changing layout will close some charts. Do you want to proceed?",
                    onDismiss = chartWindow::onLayoutChangeCancelled,
                    onConfirm = chartWindow::onLayoutChangeConfirmed,
                )
            }
        }
    }
}

@Composable
private fun StockChartScreen(
    chartWindow: StockChartWindow,
    selectedStockChart: StockChart,
    getStockChartOrNull: (Int) -> StockChart?,
    decorationType: StockChartDecorationType,
    onOpenTickerSelection: () -> Unit,
    onOpenTimeframeSelection: () -> Unit,
    onGoToDateTime: (LocalDateTime?) -> Unit,
    onGoToLatest: () -> Unit,
    layout: ChartsLayout,
    onSetLayout: (ChartsLayout) -> Unit,
    syncPrefs: StockChartsSyncPrefs,
    onToggleSyncCrosshair: (Boolean?) -> Unit,
    onToggleSyncTime: (Boolean?) -> Unit,
    onToggleSyncDateRange: (Boolean?) -> Unit,
    onNewWindow: () -> Unit,
) {

    Column {

        StockChartTopBar(
            stockChart = selectedStockChart,
            decorationType = decorationType,
            onOpenTickerSelection = onOpenTickerSelection,
            onOpenTimeframeSelection = onOpenTimeframeSelection,
            onGoToDateTime = onGoToDateTime,
            onGoToLatest = onGoToLatest,
            layout = layout,
            onSetLayout = onSetLayout,
            syncPrefs = syncPrefs,
            onToggleSyncCrosshair = onToggleSyncCrosshair,
            onToggleSyncTime = onToggleSyncTime,
            onToggleSyncDateRange = onToggleSyncDateRange,
            onNewWindow = onNewWindow,
        )

        HorizontalDivider()

        AnimatedVisibility(layout is Tabs) {

            // Tabs
            StockChartTabRow(
                chartWindow = chartWindow,
                chartIds = chartWindow.chartIds,
                selectedIndex = chartWindow.selectedChartIndex,
                title = chartWindow::getChartTitle,
            )
        }

        Box(Modifier.weight(1F).fillMaxWidth()) {

            val chartInteraction = chartWindow.chartInteraction

            // Chart page
            ChartPage(
                modifier = Modifier
                    .onSizeChanged { size -> chartInteraction.size = size.toSize() }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                chartInteraction.onEvent(awaitPointerEvent())
                            }
                        }
                    },
                state = chartWindow.pageState,
            )

            ChartOverlay(
                modifier = Modifier.matchParentSize(),
                selectedChartIndex = chartWindow.selectedChartIndex,
                layout = chartWindow.layout,
            ) { chartIndex ->

                val plotterManager = getStockChartOrNull(chartIndex)?.plotterManager ?: return@ChartOverlay

                Legend(plotterManager)
            }
        }

        HorizontalDivider()

        // Replay Controls
        if (decorationType is StockChartDecorationType.BarReplay) {

            decorationType.customControls(selectedStockChart)
        }
    }
}

private fun chartKeyboardShortcuts(
    window: StockChartWindow,
    keyEvent: KeyEvent,
    onSetFilterChar: (Char) -> Unit,
): Boolean = with(keyEvent) {

    if (window.selectedChartId == null) return false
    if (window.showTickerSelectionDialog || window.showTimeframeSelectionDialog) return false

    val defaultCondition = with(keyEvent) { isAltPressed || isMetaPressed || isShiftPressed }.not()
    if (!defaultCondition) return false

    // Ticker/Timeframe dialogs
    if (isTypedEvent) {
        val char = utf16CodePoint.toChar()
        if (char.isLetter()) window.showTickerSelectionDialog = true
        if (char.isDigit()) window.showTimeframeSelectionDialog = true
        onSetFilterChar(char)
        return true
    }

    // Chart Navigation
    if (keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown) {

        when (keyEvent.key) {
            Key.Tab if keyEvent.isShiftPressed -> window.onSelectPreviousChart()
            Key.Tab -> window.onSelectNextChart()
            Key.T -> window.onNewChart()
            Key.W -> window.onCloseCurrentChart()
            else -> return false
        }

        return true
    }

    return false
}
