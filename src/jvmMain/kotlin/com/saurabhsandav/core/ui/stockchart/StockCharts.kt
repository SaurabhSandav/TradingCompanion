package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabRow

@Composable
fun StockCharts(
    pageState: ChartPageState,
    modifier: Modifier = Modifier,
    tabsState: StockChartTabsState? = null,
    controls: @Composable ColumnScope.() -> Unit,
) {

    Row(
        modifier = modifier.fillMaxSize().chartKeyboardShortcuts(tabsState)
    ) {

        // Controls
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            content = controls,
        )

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
