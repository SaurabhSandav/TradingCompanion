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
import com.saurabhsandav.core.ui.stockchart.ChartId
import com.saurabhsandav.core.ui.stockchart.StockChartWindow

@Composable
internal fun StockChartTabRow(
    chartWindow: StockChartWindow,
    chartIds: List<ChartId>,
    selectedIndex: Int,
    title: (ChartId) -> String,
) {

    Row(Modifier.fillMaxWidth()) {

        PrimaryScrollableTabRow(
            modifier = Modifier.weight(1F),
            selectedTabIndex = selectedIndex,
            indicator = {

                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = selectedIndex,
                        matchContentSize = true,
                    ),
                    width = Dp.Unspecified,
                )
            },
        ) {

            chartIds.forEachIndexed { index, chartId ->

                key(chartId) {

                    ChartTab(
                        title = title(chartId),
                        isSelected = index == selectedIndex,
                        isCloseable = chartIds.size != 1,
                        onSelect = { chartWindow.onSelectChart(chartId) },
                        onClose = { chartWindow.onCloseChart(chartId) },
                    )
                }
            }
        }

        StockChartTabControlsRow(chartWindow = chartWindow)
    }
}

@Composable
private fun StockChartTabControlsRow(chartWindow: StockChartWindow) {

    IconButtonWithTooltip(
        onClick = chartWindow::onNewChart,
        tooltipText = "New Tab",
        content = {
            Icon(Icons.Default.Add, contentDescription = "New Tab")
        },
    )

    IconButtonWithTooltip(
        onClick = chartWindow::onSelectPreviousChart,
        tooltipText = "Previous tab",
        content = {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous tab")
        },
    )

    IconButtonWithTooltip(
        onClick = chartWindow::onSelectNextChart,
        tooltipText = "Next tab",
        content = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next tab")
        },
    )

    IconButtonWithTooltip(
        onClick = chartWindow::onMoveChartBackward,
        tooltipText = "Move tab backward",
        content = {
            Icon(Icons.Default.FastRewind, contentDescription = "Move tab backward")
        },
    )

    IconButtonWithTooltip(
        onClick = chartWindow::onMoveChartForward,
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
    onClose: () -> Unit,
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
                        onClick = onClose,
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
