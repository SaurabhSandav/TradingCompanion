package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import com.saurabhsandav.core.ui.stockchart.ui.NewChartForm
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

                Box {

                    val selectedStockChart = remember(chartWindow.selectedChartId) {
                        chartWindow.selectedChartId?.let(state::getStockChart)
                    }

                    val tickers by state.marketDataProvider.symbols().collectAsState()
                    val timeframes by state.marketDataProvider.timeframes().collectAsState()

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
                            tickers = tickers,
                            onChangeTicker = { ticker -> state.onChangeTicker(chartWindow, ticker) },
                            timeframes = timeframes,
                            onChangeTimeframe = { timeframe -> state.onChangeTimeframe(chartWindow, timeframe) },
                            onNewWindow = { state.newWindow(chartWindow) },
                            onOpenInNewTab = { ticker, timeframe ->
                                state.onOpenInNewTab(chartWindow, ticker, timeframe)
                            },
                            onGoToDateTime = { dateTime -> state.goToDateTime(chartWindow, dateTime) },
                            snackbarHost = snackbarHost,
                            customControls = customControls,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockChartScreen(
    chartWindow: StockChartWindow,
    stockChart: StockChart,
    tickers: List<String>,
    onChangeTicker: (String) -> Unit,
    timeframes: List<Timeframe>,
    onChangeTimeframe: (Timeframe) -> Unit,
    onNewWindow: () -> Unit,
    onOpenInNewTab: (String, Timeframe) -> Unit,
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
                tickers = tickers,
                onChangeTicker = onChangeTicker,
                timeframes = timeframes,
                onChangeTimeframe = onChangeTimeframe,
                onOpenInNewTab = onOpenInNewTab,
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
