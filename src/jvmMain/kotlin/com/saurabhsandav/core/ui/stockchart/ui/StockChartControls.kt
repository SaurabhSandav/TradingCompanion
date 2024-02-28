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
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.utils.nowIn
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

@Composable
internal fun StockChartControls(
    stockChart: StockChart,
    tickers: List<String>,
    onChangeTicker: (String) -> Unit,
    timeframes: List<Timeframe>,
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

                HorizontalDivider()
            }

            Column {

                stockChart.plotters.forEach { plotter ->

                    key(plotter) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {

                            Text(plotter.legendLabel)

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
                        checked = stockChart.markersAreEnabled.collectAsState(false).value,
                        onCheckedChange = { stockChart.setMarkersAreEnabled(it) },
                    )
                }
            }

            HorizontalDivider()

            OutlinedListSelectionField(
                items = tickers,
                itemText = { it },
                selection = stockChart.params.ticker,
                onSelection = onChangeTicker,
                label = { Text("Ticker") },
            )

            OutlinedListSelectionField(
                items = timeframes,
                itemText = { it.toLabel() },
                selection = stockChart.params.timeframe,
                onSelection = onChangeTimeframe,
                label = { Text("Timeframe") },
            )

            HorizontalDivider()

            var goToDate by state {
                Clock.System.nowIn(TimeZone.currentSystemDefault())
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
            isCollapsedAC -> IconButtonWithTooltip(
                modifier = Modifier.width(56.dp).fillMaxHeight().clickable(onClick = onExpandRequest),
                onClick = onExpandRequest,
                tooltipText = "Open controls",
                content = {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open controls")
                }
            )

            else -> content()
        }
    }
}
