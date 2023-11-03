package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.intState
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Composable
internal fun StockChartTabRow(
    state: StockChartTabsState,
    onNewWindow: () -> Unit,
) {

    Row(Modifier.fillMaxWidth()) {

        ScrollableTabRow(
            modifier = Modifier.weight(1F),
            selectedTabIndex = state.selectedTabIndex,
            indicator = { tabPositions ->

                var prevSelectedTabIndex by intState { 0 }

                val selectedTabIndex = state.selectedTabIndex

                val index = when {
                    selectedTabIndex < tabPositions.size -> {
                        prevSelectedTabIndex = selectedTabIndex
                        selectedTabIndex
                    }

                    else -> prevSelectedTabIndex
                }

                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[index])
                )
            }
        ) {

            state.tabIds.forEachIndexed { index, tabId ->

                key(tabId) {

                    ChartTab(
                        title = state.title(tabId),
                        isSelected = index == state.selectedTabIndex,
                        isCloseable = state.tabIds.size != 1,
                        onSelect = { state.selectTab(tabId) },
                        onCloseChart = { state.closeTab(tabId) },
                    )
                }
            }
        }

        StockChartTabControlsRow(
            state = state,
            onNewWindow = onNewWindow,
        )
    }
}

@Composable
private fun StockChartTabControlsRow(
    state: StockChartTabsState,
    onNewWindow: () -> Unit,
) {

    IconButton(
        onClick = state::newTab,
        content = {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        }
    )

    IconButton(
        onClick = state::selectPreviousTab,
        content = {
            Icon(Icons.Default.ArrowBack, contentDescription = "Previous tab")
        }
    )

    IconButton(
        onClick = state::selectNextTab,
        content = {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next tab")
        }
    )

    IconButton(
        onClick = state::moveTabBackward,
        content = {
            Icon(Icons.Default.FastRewind, contentDescription = "Move tab backward")
        }
    )

    IconButton(
        onClick = state::moveTabForward,
        content = {
            Icon(Icons.Default.FastForward, contentDescription = "Move tab forward")
        }
    )

    IconButton(
        onClick = onNewWindow,
        content = {
            Icon(Icons.Default.OpenInBrowser, contentDescription = "New window")
        }
    )
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
