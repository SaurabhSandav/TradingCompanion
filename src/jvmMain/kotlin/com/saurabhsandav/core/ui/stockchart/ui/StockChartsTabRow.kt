package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Composable
fun StockChartsTabRow(
    state: StockChartTabsState,
) {

    ScrollableTabRow(
        selectedTabIndex = state.selectedTabIndex,
    ) {

        state.tabs.forEachIndexed { index, tab ->

            key(tab.id) {

                ChartTab(
                    title = tab.title,
                    isSelected = index == state.selectedTabIndex,
                    isCloseable = state.tabs.size != 1,
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
    isCloseable: Boolean,
    onSelect: () -> Unit,
    onCloseChart: () -> Unit,
) {

    Tab(
        modifier = Modifier.height(48.dp),
        selected = isSelected,
        onClick = onSelect,
        content = {

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(title)

                AnimatedVisibility(isCloseable) {

                    IconButton(onClick = onCloseChart) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    )
}
