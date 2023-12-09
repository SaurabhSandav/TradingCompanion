package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    openTrades: ImmutableList<TradeEntry>,
    todayTrades: ImmutableList<TradeEntry>,
    pastTrades: ImmutableList<TradeEntry>,
    onOpenDetails: (ProfileTradeId) -> Unit,
    onOpenChart: (ProfileTradeId) -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
        addColumnText("ID", width = Fixed(48.dp)) { it.profileTradeId.tradeId.toString() }
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
    ) {

        tradeRows(
            trades = openTrades,
            title = "Open",
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
            keyPrefix = "open",
        )

        tradeRows(
            trades = todayTrades,
            title = "Today",
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
        )

        tradeRows(
            trades = pastTrades,
            title = "Past",
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
        )
    }
}

private fun TableScope<TradeEntry>.tradeRows(
    trades: ImmutableList<TradeEntry>,
    title: String,
    onOpenDetails: (ProfileTradeId) -> Unit,
    onOpenChart: (ProfileTradeId) -> Unit,
    keyPrefix: String? = null,
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
            )

            Divider()
        }

        rows(
            items = trades,
            key = { entry -> if (keyPrefix != null) keyPrefix + entry.profileTradeId else entry.profileTradeId },
            contentType = { ContentType.Entry },
        ) { entry ->

            TradeEntry(
                schema = schema,
                entry = entry,
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
                onOpenChart = { onOpenChart(entry.profileTradeId) },
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

            Divider()
        }
    }
}

private enum class ContentType {
    Header, Entry;
}
