package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.stockchart.ui.*
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionDialog
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
import kotlinx.datetime.LocalDateTime

@Composable
fun StockCharts(
    state: StockChartsState,
    windowTitle: String,
    onCloseRequest: () -> Unit,
    snackbarHost: (@Composable () -> Unit)? = null,
    customShortcuts: ((KeyEvent) -> Boolean)? = null,
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
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
                            onNewWindow = { state.newWindow(chartWindow) },
                            onGoToDateTime = { dateTime -> state.goToDateTime(chartWindow, dateTime) },
                            snackbarHost = snackbarHost,
                            customControls = customControls,
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
    onNewWindow: () -> Unit,
    onGoToDateTime: (LocalDateTime?) -> Unit,
    snackbarHost: (@Composable () -> Unit)?,
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)?,
) {

    Box {

        Row(
            modifier = Modifier.fillMaxSize()
        ) {

            // Controls
            StockChartControls(
                stockChart = stockChart,
                onGoToDateTime = onGoToDateTime,
                customControls = customControls,
            )

            Column {

                // Tabs
                StockChartTabRow(
                    state = chartWindow.tabsState,
                    onNewWindow = onNewWindow,
                )

                // Chart page
                ChartPage(
                    state = chartWindow.pageState,
                    modifier = Modifier.fillMaxSize(),
                    legend = { Legend(stockChart) },
                )
            }
        }

        if (snackbarHost != null) {

            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
                propagateMinConstraints = true,
                content = { snackbarHost() }
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
