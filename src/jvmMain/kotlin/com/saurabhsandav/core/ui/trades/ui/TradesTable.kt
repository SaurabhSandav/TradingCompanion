package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trades.model.TradesState.*

@Composable
internal fun TradesTable(
    tradesList: TradesList,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
    onSetFocusModeEnabled: (Boolean) -> Unit,
    onFilter: () -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
        addColumnText("ID", width = Fixed(48.dp)) { it.id.toString() }
        addColumnText("Broker", width = Weight(2F)) { it.broker }
        addColumnText("Ticker", width = Weight(1.7F)) { it.ticker }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Quantity") { it.quantity }
        addColumnText("Avg. Entry") { it.entry }
        addColumnText("Avg. Exit") { it.exit ?: "NA" }
        addColumnText("Entry Time", width = Weight(2.2F)) { it.entryTime }
        addColumn("Duration", width = Weight(1.5F)) {

            Text(
                text = it.duration.collectAsState("").value,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        addColumn("PNL") {
            Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumn("Net PNL") {
            Text(it.netPnl, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Fees") { it.fees }
    }

    LazyTable(
        schema = schema,
        headerContent = {

            Column {

                DefaultTableHeader(schema)

                HorizontalDivider()

                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = MaterialTheme.dimens.rowHorizontalSpacing,
                        alignment = Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    SingleChoiceSegmentedButtonRow {

                        val isFocusMode = tradesList is TradesList.Focused

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onSetFocusModeEnabled(false) },
                            selected = !isFocusMode,
                            label = { Text("All") },
                        )

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { onSetFocusModeEnabled(true) },
                            selected = isFocusMode,
                            label = { Text("Focus") },
                        )
                    }

                    Spacer(Modifier.weight(1F))

                    OutlinedButton(
                        onClick = onFilter,
                        shape = MaterialTheme.shapes.small,
                        content = { Text("Filter") },
                    )
                }

                HorizontalDivider()
            }
        },
    ) {

        when (tradesList) {
            is TradesList.All ->

                tradeRows(
                    trades = tradesList.trades,
                    title = if (tradesList.isFiltered) "Filtered" else "All",
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                )

            is TradesList.Focused -> {

                tradeRows(
                    trades = tradesList.openTrades,
                    title = "Open",
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                )

                tradeRows(
                    trades = tradesList.todayTrades,
                    title = "Today",
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                    stats = tradesList.todayStats,
                )

                tradeRows(
                    trades = tradesList.pastTrades,
                    title = "Past",
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                )
            }
        }
    }
}

private fun TableScope<TradeEntry>.tradeRows(
    trades: List<TradeEntry>,
    title: String,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
    stats: Stats? = null,
) {

    if (trades.isNotEmpty()) {

        row(
            contentType = ContentType.Header,
        ) {

            ListItem(
                modifier = Modifier.padding(16.dp),
                headlineContent = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = "${trades.size} Trades",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                trailingContent = stats?.let { { StatsHeaderItem(it) } },
            )

            HorizontalDivider()
        }

        rows(
            items = trades,
            key = { entry -> entry.id },
            contentType = { ContentType.Entry },
        ) { entry ->

            TradeEntry(
                schema = schema,
                entry = entry,
                onOpenDetails = { onOpenDetails(entry.id) },
                onOpenChart = { onOpenChart(entry.id) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
    schema: TableSchema<TradeEntry>,
    entry: TradeEntry,
    onOpenDetails: () -> Unit,
    onOpenChart: () -> Unit,
) {

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Details", onOpenDetails),
                ContextMenuItem("Chart", onOpenChart),
            )
        },
    ) {

        Column {

            DefaultTableRow(
                item = entry,
                schema = schema,
                onClick = onOpenDetails,
            )

            HorizontalDivider()
        }
    }
}

@Composable
private fun StatsHeaderItem(stats: Stats) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = stats.pnl,
                style = MaterialTheme.typography.titleMedium,
                color = if (stats.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = "PNL",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = stats.netPnl,
                style = MaterialTheme.typography.titleMedium,
                color = if (stats.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = "Net PNL",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private enum class ContentType {
    Header, Entry;
}
