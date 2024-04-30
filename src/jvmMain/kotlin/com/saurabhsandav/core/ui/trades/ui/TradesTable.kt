package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table2.*
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Weight
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

    LazyTable(
        headerContent = {

            TradeTableSchema.SimpleHeader {
                id.text { "ID" }
                broker.text { "Broker" }
                ticker.text { "Ticker" }
                side.text { "Side" }
                quantity.text { "Quantity" }
                avgEntry.text { "Avg. Entry" }
                avgExit.text { "Avg. Exit" }
                entryTime.text { "Entry Time" }
                duration.text { "Duration" }
                pnl.text { "PNL" }
                netPnl.text { "Net PNL" }
                fees.text { "Fees" }
            }

            Row(
                modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
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
        },
    ) {

        when (tradesList) {
            is TradesList.All -> {

                tradeRows(
                    trades = tradesList.trades,
                    title = if (tradesList.isFiltered) "Filtered" else "All",
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                )
            }

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

private fun LazyListScope.tradeRows(
    trades: List<TradeEntry>,
    title: String,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
    stats: Stats? = null,
) {

    if (trades.isNotEmpty()) {

        item(
            contentType = ContentType.Header,
        ) {

            ListItem(
                modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding),
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

        items(
            items = trades,
            key = { entry -> entry.id },
            contentType = { ContentType.Entry },
        ) { entry ->

            TradeEntry(
                entry = entry,
                onOpenDetails = { onOpenDetails(entry.id) },
                onOpenChart = { onOpenChart(entry.id) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
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

            TradeTableSchema.SimpleRow(
                onClick = onOpenDetails,
            ) {
                id.text { entry.id.toString() }
                broker.text { entry.broker }
                ticker.text { entry.ticker }
                side {
                    Text(entry.side, color = if (entry.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { entry.quantity }
                avgEntry.text { entry.entry }
                avgExit.text { entry.exit ?: "NA" }
                entryTime.text { entry.entryTime }
                duration {

                    Text(
                        text = entry.duration.collectAsState("").value,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl {
                    Text(entry.pnl, color = if (entry.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl {
                    Text(entry.netPnl, color = if (entry.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                fees.text { entry.fees }
            }

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

private object TradeTableSchema : TableSchema() {

    val id = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val entryTime = cell(Weight(2.2F))
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
    val fees = cell()
}
