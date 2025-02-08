package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.ListSelectionFieldDefaults
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionField
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
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
    onOpenInNewTab: (String, Timeframe) -> Unit,
    onGoToDateTime: (LocalDateTime?) -> Unit,
    customControls: (@Composable ColumnScope.(StockChart) -> Unit)? = null,
) {

    var isCollapsed by state { false }

    CollapsiblePane(
        isCollapsed = isCollapsed,
        onExpandRequest = { isCollapsed = false },
    ) {

        Column(
            modifier = Modifier.width(250.dp)
                .fillMaxHeight()
                .padding(MaterialTheme.dimens.containerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.dimens.columnVerticalSpacing,
                alignment = Alignment.CenterVertically,
            ),
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isCollapsed = true },
                content = { Text("Hide Pane") },
            )

            if (customControls != null)
                customControls(stockChart)

            HorizontalDivider()

            TickerSelectionField(
                type = TickerSelectionType.Chart(
                    onOpenInNewTab = { ticker -> onOpenInNewTab(ticker, stockChart.params.timeframe) }
                ),
                tickers = tickers,
                selected = stockChart.params.ticker,
                onSelect = onChangeTicker,
            )

            OutlinedListSelectionField(
                items = timeframes,
                itemText = { it.toLabel() },
                selection = stockChart.params.timeframe,
                onSelect = onChangeTimeframe,
                label = { Text("Timeframe") },
                trailingIcon = ListSelectionFieldDefaults.TrailingIcon(
                    icon = Icons.AutoMirrored.Default.OpenInNew,
                    contentDescription = "Open in new tab",
                    onClick = { onOpenInNewTab(stockChart.params.ticker, it) }
                ),
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
