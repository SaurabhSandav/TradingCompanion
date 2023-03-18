package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun StockChartControls(
    stockChart: StockChart,
    onChangeTicker: (String) -> Unit,
    onChangeTimeframe: (Timeframe) -> Unit,
    onGoToDateTime: (LocalDateTime?) -> Unit,
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
) {

    var isCollapsed by state { false }

    CollapsiblePane(
        isCollapsed = isCollapsed,
        onExpandRequest = { isCollapsed = false },
    ) {

        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isCollapsed = true },
                content = { Text("Hide Pane") },
            )

            if (customControls != null) {

                customControls(stockChart)

                Divider()
            }

            Column {

                stockChart.plotters.forEach { plotter ->

                    key(plotter) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {

                            Text(plotter.name)

                            Switch(
                                checked = plotter.isEnabled,
                                onCheckedChange = { stockChart.setPlotterIsEnabled(plotter, it) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {

                    Text("Markers")

                    Switch(
                        checked = stockChart.markersAreEnabled,
                        onCheckedChange = { stockChart.markersAreEnabled = it },
                    )
                }
            }

            Divider()

            ListSelectionField(
                items = NIFTY50,
                selection = stockChart.currentParams?.ticker,
                onSelection = onChangeTicker,
                label = { Text("Ticker") },
            )

            ListSelectionField(
                items = remember { Timeframe.values().map { it.toLabel() }.toImmutableList() },
                selection = stockChart.currentParams?.timeframe?.toLabel(),
                onSelection = { onChangeTimeframe(timeframeFromLabel(it)) },
                label = { Text("Timeframe") },
            )

            Divider()

            var goToDate by state {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }

            DateTimeField(
                value = goToDate,
                onValidValueChange = { goToDate = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {

                Button(onClick = { onGoToDateTime(null) }) {
                    Text("Now")
                }

                Button(onClick = { onGoToDateTime(goToDate) }) {
                    Text("Go")
                }
            }
        }
    }
}

@Composable
private fun CollapsiblePane(
    isCollapsed: Boolean,
    onExpandRequest: () -> Unit,
    content: @Composable () -> Unit,
) {

    AnimatedContent(
        targetState = isCollapsed,
    ) { isCollapsedAC ->

        when {
            isCollapsedAC -> IconButton(
                modifier = Modifier.width(56.dp).fillMaxHeight().clickable(onClick = onExpandRequest),
                onClick = onExpandRequest,
                content = {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open controls")
                }
            )

            else -> content()
        }
    }
}
