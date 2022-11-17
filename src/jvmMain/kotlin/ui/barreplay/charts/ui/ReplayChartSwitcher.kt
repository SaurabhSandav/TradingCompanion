package ui.barreplay.charts.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartTabsState
import ui.common.ResizableChart

@Composable
fun ReplayChartSwitcher(
    chartTabsState: ReplayChartTabsState,
    chartState: ReplayChartState,
    onSelectChart: (Int) -> Unit,
    onCloseChart: (Int) -> Unit,
) {

    Column {

        ScrollableTabRow(
            selectedTabIndex = chartTabsState.selectedTabIndex,
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

        key(chartState.id) {

            ResizableChart(
                state = chartState.state,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
