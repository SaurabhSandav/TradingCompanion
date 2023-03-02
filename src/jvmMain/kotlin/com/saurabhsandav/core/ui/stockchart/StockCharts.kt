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
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabControls
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabRow
import com.saurabhsandav.core.utils.NIFTY50

@Composable
internal fun StockCharts(
    pageState: ChartPageState,
    stockChart: StockChart?,
    onTickerChange: (String) -> Unit,
    onTimeframeChange: (Timeframe) -> Unit,
    modifier: Modifier = Modifier,
    tabsState: StockChartTabsState? = null,
    controls: (@Composable ColumnScope.() -> Unit)? = null,
) {

    Row(
        modifier = modifier.fillMaxSize().chartKeyboardShortcuts(tabsState)
    ) {

        // Controls
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            controls?.let {

                it()

                Divider()
            }

            ListSelectionField(
                items = NIFTY50,
                selection = stockChart?.currentParams?.ticker,
                onSelection = onTickerChange,
                label = { Text("Ticker") },
            )

            ListSelectionField(
                items = remember { Timeframe.values().map { it.toLabel() } },
                selection = stockChart?.currentParams?.timeframe?.toLabel(),
                onSelection = { onTimeframeChange(timeframeFromLabel(it)) },
                label = { Text("Timeframe") },
            )

            tabsState?.let {

                Divider()

                StockChartsTabControls(it)
            }
        }

        Column {

            // Tabs
            tabsState?.let {

                StockChartsTabRow(it)
            }

            // Chart page
            ChartPage(
                state = pageState,
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
