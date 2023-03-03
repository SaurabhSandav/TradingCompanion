package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabRow
import com.saurabhsandav.core.utils.NIFTY50

@Composable
internal fun StockCharts(
    state: StockChartsState,
    modifier: Modifier = Modifier,
    controls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
) {

    Row(
        modifier = modifier.fillMaxSize().chartKeyboardShortcuts(state.tabsState)
    ) {

        // Controls
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            val stockChart = state.currentStockChart

            if (controls != null && stockChart != null) {

                controls(stockChart)

                Divider()
            }

            ListSelectionField(
                items = NIFTY50,
                selection = stockChart?.currentParams?.ticker,
                onSelection = { state.changeTicker(it) },
                label = { Text("Ticker") },
                enabled = stockChart != null,
            )

            ListSelectionField(
                items = remember { Timeframe.values().map { it.toLabel() } },
                selection = stockChart?.currentParams?.timeframe?.toLabel(),
                onSelection = { state.changeTimeframe(timeframeFromLabel(it)) },
                label = { Text("Timeframe") },
                enabled = stockChart != null,
            )

            Divider()

            StockChartsTabControls(state.tabsState)
        }

        Column {

            // Tabs
            StockChartsTabRow(state.tabsState)

            // Chart page
            ChartPage(
                state = state.pageState,
                modifier = Modifier.weight(1F),
            )
        }
    }
}

private fun Modifier.chartKeyboardShortcuts(tabsState: StockChartTabsState?): Modifier {
    return then(
        if (tabsState == null) Modifier else Modifier.onPreviewKeyEvent { keyEvent ->

            when {
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
        }
    )
}
