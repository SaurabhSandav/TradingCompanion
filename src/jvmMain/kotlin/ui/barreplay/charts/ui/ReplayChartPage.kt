package ui.barreplay.charts.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartPageState
import ui.stockchart.StockChartTabsState

@Composable
fun ReplayChartPage(
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
) {

    Column {

        ScrollableTabRow(
            selectedTabIndex = tabsState.selectedTabIndex,
            indicator = { tabPositions ->
                val selectedTabIndex = tabsState.selectedTabIndex
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                    )
                }
            }
        ) {

            tabsState.tabs.forEachIndexed { index, chartTab ->

                key(chartTab.id) {

                    ReplayTab(
                        title = chartTab.title,
                        isSelected = index == tabsState.selectedTabIndex,
                        onSelect = { tabsState.selectTab(chartTab.id) },
                        onCloseChart = { tabsState.selectTab(chartTab.id) },
                    )
                }
            }

            Tab(
                selected = false,
                onClick = tabsState::newTab,
            ) {

                IconButton(
                    onClick = tabsState::newTab,
                    content = {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                )
            }
        }

        ChartPage(
            state = chartPageState,
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
