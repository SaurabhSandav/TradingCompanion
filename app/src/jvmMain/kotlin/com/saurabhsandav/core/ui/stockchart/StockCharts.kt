package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.chart.SimpleChart
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.stockchart.ui.Legend
import com.saurabhsandav.core.ui.stockchart.ui.NewChartForm
import com.saurabhsandav.core.ui.stockchart.ui.StockChartControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTabRow
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTopBar
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
    snackbarHost: (@Composable () -> Unit)? = null,
    customShortcuts: ((KeyEvent) -> Boolean)? = null,
) {

    state.windows.forEach { chartWindow ->

        key(chartWindow) {

            var initialFilterQuery by state<String> { "" }

            AppWindow(
                title = windowTitle,
                onCloseRequest = { if (!state.closeWindow(chartWindow)) onCloseRequest() },
                state = rememberAppWindowState(preferredPlacement = WindowPlacement.Maximized),
                onPreviewKeyEvent = { keyEvent ->

                    when {
                        chartKeyboardShortcuts(chartWindow, keyEvent) { initialFilterQuery = it.toString() } -> true
                        else -> customShortcuts?.invoke(keyEvent) == true
                    }
                },
            ) {

                chartWindow.appWindowState = LocalAppWindowState.current

                val tickers by state.marketDataProvider.symbols().collectAsState()
                val timeframes by state.marketDataProvider.timeframes().collectAsState()

                Box {

                    val selectedStockChart = remember(chartWindow.selectedChartId) {
                        chartWindow.selectedChartId?.let(state::getStockChart)
                    }

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
                            stockChart = selectedStockChart,
                            onChartActive = { state.onChartActive(selectedStockChart.chartId) },
                            decorationType = decorationType,
                            onOpenTickerSelection = { chartWindow.showTickerSelectionDialog = true },
                            onOpenTimeframeSelection = { chartWindow.showTimeframeSelectionDialog = true },
                            onGoToDateTime = { dateTime -> state.goToDateTime(chartWindow, dateTime) },
                            onGoToLatest = { state.goToLatest(chartWindow) },
                            onNewWindow = { state.newWindow(chartWindow) },
                            snackbarHost = snackbarHost,
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
                            onOpenInNewTab = { ticker -> state.onOpenInNewTab(chartWindow, ticker, null) },
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
                        onOpenInNewTab = { timeframe -> state.onOpenInNewTab(chartWindow, null, timeframe) },
                        onOpenInNewWindow = { timeframe -> state.onOpenInNewWindow(chartWindow, null, timeframe) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StockChartScreen(
    chartWindow: StockChartWindow,
    stockChart: StockChart,
    onChartActive: () -> Unit,
    decorationType: StockChartDecorationType,
    onOpenTickerSelection: () -> Unit,
    onOpenTimeframeSelection: () -> Unit,
    onGoToDateTime: (LocalDateTime?) -> Unit,
    onGoToLatest: () -> Unit,
    onNewWindow: () -> Unit,
    snackbarHost: (@Composable () -> Unit)?,
) {

    Box {

        Column {

            StockChartTopBar(
                stockChart = stockChart,
                decorationType = decorationType,
                onOpenTickerSelection = onOpenTickerSelection,
                onOpenTimeframeSelection = onOpenTimeframeSelection,
                onGoToDateTime = onGoToDateTime,
                onGoToLatest = onGoToLatest,
                onNewWindow = onNewWindow,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxSize(),
            ) {

                // Replay Controls
                if (decorationType is StockChartDecorationType.BarReplay) {

                    StockChartControls(
                        stockChart = stockChart,
                        customControls = decorationType.customControls,
                    )
                }

                Column {

                    // Tabs
                    StockChartTabRow(
                        state = chartWindow.tabsState,
                    )

                    // Chart page
                    SimpleChart(
                        modifier = Modifier
                            .weight(1F)
                            .onPointerEvent(PointerEventType.Enter) { onChartActive() },
                        pageState = chartWindow.pageState,
                        legend = { Legend(stockChart.plotterManager) },
                    )
                }
            }
        }

        if (snackbarHost != null) {

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                propagateMinConstraints = true,
                content = { snackbarHost() },
            )
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

    if (isTypedEvent) {
        val char = utf16CodePoint.toChar()
        if (char.isLetter()) window.showTickerSelectionDialog = true
        if (char.isDigit()) window.showTimeframeSelectionDialog = true
        onSetFilterChar(char)
        return@with true
    }

    val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
    if (!defaultCondition) return false

    val tabsState = window.tabsState

    when (keyEvent.key) {
        Key.Tab if keyEvent.isShiftPressed -> tabsState.selectPreviousTab()
        Key.Tab -> tabsState.selectNextTab()
        Key.T -> tabsState.newTab()
        Key.W -> tabsState.closeCurrentTab()
        else -> return false
    }

    return true
}
