package com.saurabhsandav.core.ui.charts.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.MarkedTradeEntry
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MarkedTradesTable(
    markedTrades: ImmutableList<MarkedTradeEntry>,
    onUnMarkTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
) {

    val schema = rememberTableSchema<MarkedTradeEntry> {
        addColumn("Mark") { tradeEntry ->
            Checkbox(
                checked = true,
                onCheckedChange = { onUnMarkTrade(tradeEntry.profileTradeId) }
            )
        }
        addColumnText("Profile") { it.profileName }
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Quantity") { it.quantity }
        addColumnText("Avg. Entry") { it.entry }
        addColumnText("Avg. Exit") { it.exit ?: "NA" }
        addColumn("Duration") {

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
    ) {

        rows(
            items = markedTrades,
            key = { it.profileTradeId },
        ) { entry ->

            MarkedTradeEntry(
                schema = schema,
                entry = entry,
                onSelectTrade = { onSelectTrade(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun MarkedTradeEntry(
    schema: TableSchema<MarkedTradeEntry>,
    entry: MarkedTradeEntry,
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

            Divider()
        }
    }
}
