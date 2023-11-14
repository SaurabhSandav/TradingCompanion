package com.saurabhsandav.core.ui.review.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    trades: ImmutableList<TradeEntry>,
    onOpenChart: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
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
    ) {

        rows(
            items = trades,
            key = { it.profileTradeId },
        ) { entry ->

            TradeEntry(
                schema = schema,
                entry = entry,
                onOpenChart = { onOpenChart(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
    schema: TableSchema<TradeEntry>,
    entry: TradeEntry,
    onOpenChart: () -> Unit,
    onOpenDetails: () -> Unit,
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
            )

            Divider()
        }
    }
}
