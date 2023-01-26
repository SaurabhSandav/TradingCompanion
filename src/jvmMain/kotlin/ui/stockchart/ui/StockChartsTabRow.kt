package ui.stockchart.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
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
import ui.stockchart.StockChartTabsState

@Composable
fun StockChartsTabRow(
    state: StockChartTabsState,
) {

    ScrollableTabRow(
        selectedTabIndex = state.selectedTabIndex,
        indicator = { tabPositions ->
            val selectedTabIndex = state.selectedTabIndex
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                )
            }
        }
    ) {

        state.tabs.forEachIndexed { index, tab ->

            key(tab.id) {

                ChartTab(
                    title = tab.title,
                    isSelected = index == state.selectedTabIndex,
                    onSelect = { state.selectTab(tab.id) },
                    onCloseChart = { state.closeTab(tab.id) },
                )
            }
        }

        Tab(
            selected = false,
            onClick = state::newTab,
        ) {

            IconButton(
                onClick = state::newTab,
                content = {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }
            )
        }
    }
}

@Composable
private fun ChartTab(
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
