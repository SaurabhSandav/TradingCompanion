package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.stockchart.ui.Legend
import com.saurabhsandav.core.ui.stockchart.ui.StockChartControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTabRow
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

            AppWindow(
                title = windowTitle,
                onCloseRequest = { if (!state.closeWindow(chartWindow)) onCloseRequest() },
                state = rememberAppWindowState(preferredPlacement = WindowPlacement.Maximized),
                onPreviewKeyEvent = { keyEvent ->

                    when {
                        chartKeyboardShortcuts(keyEvent, chartWindow.tabsState) -> true
                        else -> customShortcuts?.invoke(keyEvent) == true
                    }
                },
            ) {

                chartWindow.appWindowState = LocalAppWindowState.current

                StockChartScreen(
                    chartWindow = chartWindow,
                    tickers = state.marketDataProvider.symbols().collectAsState().value,
                    onChangeTicker = { stockChart, ticker -> state.onChangeTicker(stockChart, ticker) },
                    timeframes = state.marketDataProvider.timeframes().collectAsState().value,
                    onChangeTimeframe = { stockChart, timeframe -> state.onChangeTimeframe(stockChart, timeframe) },
                    onNewWindow = { stockChart -> state.newWindow(stockChart) },
                    onOpenInNewTab = state::onOpenInNewTab,
                    onGoToDateTime = { stockChart, dateTime -> state.goToDateTime(stockChart, dateTime) },
                    snackbarHost = snackbarHost,
                    customControls = customControls,
                )
            }
        }
    }
}

@Composable
private fun StockChartScreen(
    chartWindow: StockChartWindow,
    tickers: List<String>,
    onChangeTicker: (StockChart, String) -> Unit,
    timeframes: List<Timeframe>,
    onChangeTimeframe: (StockChart, Timeframe) -> Unit,
    onNewWindow: (StockChart) -> Unit,
    onOpenInNewTab: (String, Timeframe) -> Unit,
    onGoToDateTime: (StockChart, LocalDateTime?) -> Unit,
    snackbarHost: (@Composable () -> Unit)?,
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)?,
) {

    Box {

        Row(
            modifier = Modifier.fillMaxSize()
        ) {

            val stockChart = chartWindow.selectedStockChart

            // Controls
            StockChartControls(
                stockChart = stockChart,
                tickers = tickers,
                onChangeTicker = { ticker -> onChangeTicker(stockChart, ticker) },
                timeframes = timeframes,
                onChangeTimeframe = { timeframe -> onChangeTimeframe(stockChart, timeframe) },
                onOpenInNewTab = onOpenInNewTab,
                onGoToDateTime = { dateTime -> onGoToDateTime(stockChart, dateTime) },
                customControls = customControls,
            )

            Column {

                // Tabs
                StockChartTabRow(
                    state = chartWindow.tabsState,
                    onNewWindow = { onNewWindow(stockChart) },
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
    keyEvent: KeyEvent,
    tabsState: StockChartTabsState,
): Boolean {

    val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown

    if (!defaultCondition) return false

    when (keyEvent.key) {
        Key.Tab if keyEvent.isShiftPressed -> tabsState.selectPreviousTab()
        Key.Tab -> tabsState.selectNextTab()
        Key.T -> tabsState.newTab()
        Key.W -> tabsState.closeCurrentTab()
        else -> return false
    }

    return true
}
