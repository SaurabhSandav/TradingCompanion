package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.stockchart.ui.StockChartControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartTabRow

@Composable
internal fun StockCharts(
    state: StockChartsState,
    windowTitle: String,
    onCloseRequest: () -> Unit,
    snackbarHost: @Composable ColumnScope.() -> Unit = {},
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
) {

    state.windows.forEach { chartWindow ->

        key(chartWindow) {

            AppWindow(
                title = windowTitle,
                onCloseRequest = { if (!state.closeWindow(chartWindow)) onCloseRequest() },
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                onPreviewKeyEvent = { keyEvent -> chartKeyboardShortcuts(keyEvent, chartWindow.tabsState) },
            ) {

                Column {

                    Row(
                        modifier = Modifier.weight(1F)
                    ) {

                        val stockChart = chartWindow.selectedStockChart

                        // Controls
                        StockChartControls(
                            stockChart = stockChart,
                            onChangeTicker = { ticker -> state.onChangeTicker(stockChart, ticker) },
                            onChangeTimeframe = { timeframe -> state.onChangeTimeframe(stockChart, timeframe) },
                            onGoToDateTime = { dateTime -> state.goToDateTime(stockChart, dateTime) },
                            customControls = customControls,
                        )

                        Column {

                            // Tabs
                            StockChartTabRow(
                                state = chartWindow.tabsState,
                                onNewWindow = { state.newWindow(stockChart) },
                            )

                            // Chart page
                            ChartPage(
                                state = chartWindow.pageState,
                                modifier = Modifier.weight(1F),
                            )
                        }
                    }

                    snackbarHost()
                }
            }
        }
    }
}

private fun chartKeyboardShortcuts(
    keyEvent: KeyEvent,
    tabsState: StockChartTabsState,
): Boolean = when {
    keyEvent.isCtrlPressed &&
            keyEvent.key == Key.Tab &&
            keyEvent.type == KeyEventType.KeyUp -> {

        when {
            keyEvent.isShiftPressed -> tabsState.selectPreviousTab()
            else -> tabsState.selectNextTab()
        }

        true
    }

    keyEvent.isCtrlPressed &&
            keyEvent.key == Key.T &&
            keyEvent.type == KeyEventType.KeyUp -> {

        tabsState.newTab()

        true
    }

    keyEvent.isCtrlPressed &&
            keyEvent.key == Key.W &&
            keyEvent.type == KeyEventType.KeyUp -> {

        tabsState.closeCurrentTab()

        true
    }

    else -> false
}
