package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeEntry

@Composable
internal fun ProfileTradesTable(
    trades: List<TradeEntry>,
    onMarkTrade: (profileTradeId: ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
    isFilterEnabled: Boolean,
    onFilter: () -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
        addColumn("Mark", width = Fixed(48.dp)) { tradeEntry ->
            Checkbox(
                checked = tradeEntry.isMarked,
                onCheckedChange = { onMarkTrade(tradeEntry.profileTradeId, it) }
            )
        }
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
    }

    LazyTable(
        schema = schema,
        headerContent = {

            Column {

                DefaultTableHeader(schema)

                HorizontalDivider()

                OutlinedButton(
                    modifier = Modifier.align(Alignment.End).padding(MaterialTheme.dimens.containerPadding),
                    onClick = onFilter,
                    shape = MaterialTheme.shapes.small,
                    enabled = isFilterEnabled,
                    content = { Text("Filter") },
                )

                HorizontalDivider()
            }
        },
    ) {

        rows(
            items = trades,
            key = { it.profileTradeId },
        ) { entry ->

            TradeEntry(
                schema = schema,
                entry = entry,
                onSelectTrade = { onSelectTrade(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
    schema: TableSchema<TradeEntry>,
    entry: TradeEntry,
    onSelectTrade: () -> Unit,
    onOpenDetails: () -> Unit,
) {

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("Open Details", onOpenDetails),
            )
        },
    ) {

        Column {

            DefaultTableRow(
                item = entry,
                schema = schema,
                onClick = onSelectTrade,
            )

            HorizontalDivider()
        }
    }
}
