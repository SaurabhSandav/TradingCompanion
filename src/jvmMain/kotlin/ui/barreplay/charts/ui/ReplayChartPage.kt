package ui.barreplay.charts.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ui.barreplay.charts.model.ReplayChartTabsState
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartState

@Composable
fun ReplayChartPage(
    chartTabsState: ReplayChartTabsState,
    chartState: ChartState,
    onSelectChart: (Int) -> Unit,
    onCloseChart: (Int) -> Unit,
) {

    Column {

        ScrollableTabRow(
            selectedTabIndex = chartTabsState.selectedTabIndex,
            indicator = { tabPositions ->
                val selectedTabIndex = chartTabsState.selectedTabIndex
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                    )
                }
            }
        ) {

            chartTabsState.tabs.forEachIndexed { index, chartTab ->

                key(chartTab.id) {

                    ReplayTab(
                        title = chartTab.title,
                        isSelected = index == chartTabsState.selectedTabIndex,
                        onSelect = { onSelectChart(chartTab.id) },
                        onCloseChart = { onCloseChart(chartTab.id) },
                    )
                }
            }
        }

        ChartPage(
            state = chartState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ReplayTab(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCloseChart: () -> Unit,
) {

    Tab(
        selected = isSelected,
        onClick = onSelect,
        content = {

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(title)

                val alpha by animateFloatAsState(if (!isSelected) 1F else 0F)

                IconButton(
                    onClick = onCloseChart,
                    modifier = Modifier.alpha(alpha),
                    enabled = !isSelected,
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    )
}
