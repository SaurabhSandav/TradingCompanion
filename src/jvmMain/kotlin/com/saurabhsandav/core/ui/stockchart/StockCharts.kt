package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabRow
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun StockCharts(
    state: StockChartsState,
    windowTitle: String,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable ColumnScope.() -> Unit = {},
    controls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
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
                        modifier = modifier.weight(1F)
                    ) {

                        // Controls
                        Column(
                            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        ) {

                            val stockChart = chartWindow.currentStockChart.value

                            if (controls != null && stockChart != null) {

                                controls(stockChart)

                                Divider()
                            }

                            Column {

                                stockChart?.plotters?.forEach { plotter ->

                                    key(plotter) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {

                                            Text(plotter.name)

                                            Switch(
                                                checked = plotter.isEnabled,
                                                onCheckedChange = { stockChart.setPlotterIsEnabled(plotter, it) },
                                            )
                                        }
                                    }
                                }

                                if (stockChart != null) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {

                                        Text("Markers")

                                        Switch(
                                            checked = stockChart.markersAreEnabled,
                                            onCheckedChange = { stockChart.markersAreEnabled = it },
                                        )
                                    }
                                }
                            }

                            Divider()

                            ListSelectionField(
                                items = NIFTY50,
                                selection = stockChart?.currentParams?.ticker,
                                onSelection = { state.changeTicker(chartWindow, it) },
                                label = { Text("Ticker") },
                                enabled = stockChart != null,
                            )

                            ListSelectionField(
                                items = remember { Timeframe.values().map { it.toLabel() }.toImmutableList() },
                                selection = stockChart?.currentParams?.timeframe?.toLabel(),
                                onSelection = { state.changeTimeframe(chartWindow, timeframeFromLabel(it)) },
                                label = { Text("Timeframe") },
                                enabled = stockChart != null,
                            )

                            Divider()

                            var goToDate by state {
                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            }

                            DateTimeField(
                                value = goToDate,
                                onValidValueChange = { goToDate = it },
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {

                                Button(onClick = { state.goToDateTime(chartWindow, null) }) {
                                    Text("Now")
                                }

                                Button(onClick = { state.goToDateTime(chartWindow, goToDate) }) {
                                    Text("Go")
                                }
                            }

                            Divider()

                            Button(
                                onClick = { state.newWindow(chartWindow) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("New window")
                            }

                            StockChartsTabControls(chartWindow.tabsState)
                        }

                        Column {

                            // Tabs
                            StockChartsTabRow(chartWindow.tabsState)

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
