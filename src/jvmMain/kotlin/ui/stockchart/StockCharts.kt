package ui.stockchart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartPageState
import ui.stockchart.ui.StockChartsTabRow

@Composable
fun StockCharts(
    pageState: ChartPageState,
    modifier: Modifier = Modifier,
    tabsState: StockChartTabsState? = null,
    controls: @Composable ColumnScope.() -> Unit,
) {

    Row(
        modifier.fillMaxSize().then(
            if (tabsState == null) Modifier else Modifier.onPreviewKeyEvent { keyEvent ->

                when {
                    keyEvent.isCtrlPressed &&
                            keyEvent.key == Key.Tab &&
                            keyEvent.type == KeyEventType.KeyDown -> {

                        when {
                            keyEvent.isShiftPressed -> tabsState.selectPreviousTab()
                            else -> tabsState.selectNextTab()
                        }

                        true
                    }

                    else -> false
                }
            }
        )) {

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
