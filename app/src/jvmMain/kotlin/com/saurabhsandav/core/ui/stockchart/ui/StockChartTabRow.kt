package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Composable
internal fun StockChartTabRow(state: StockChartTabsState) {

    Row(Modifier.fillMaxWidth()) {

        PrimaryScrollableTabRow(
            modifier = Modifier.weight(1F),
            selectedTabIndex = state.selectedTabIndex,
            indicator = {

                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = state.selectedTabIndex,
                        matchContentSize = true,
                    ),
                    width = Dp.Unspecified,
                )
            },
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

        StockChartTabControlsRow(state = state)
    }
}

@Composable
private fun StockChartTabControlsRow(state: StockChartTabsState) {

    IconButtonWithTooltip(
        onClick = state::newTab,
        tooltipText = "New Tab",
        content = {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        },
    )

    IconButtonWithTooltip(
        onClick = state::selectPreviousTab,
        tooltipText = "Previous tab",
        content = {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous tab")
        },
    )

    IconButtonWithTooltip(
        onClick = state::selectNextTab,
        tooltipText = "Next tab",
        content = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next tab")
        },
    )

    IconButtonWithTooltip(
        onClick = state::moveTabBackward,
        tooltipText = "Move tab backward",
        content = {
            Icon(Icons.Default.FastRewind, contentDescription = "Move tab backward")
        },
    )

    IconButtonWithTooltip(
        onClick = state::moveTabForward,
        tooltipText = "Move tab forward",
        content = {
            Icon(Icons.Default.FastForward, contentDescription = "Move tab forward")
        },
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

                    IconButtonWithTooltip(
                        onClick = onCloseChart,
                        tooltipText = "Close",
                        content = {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        },
                    )
                }
            }
        },
    )
}
