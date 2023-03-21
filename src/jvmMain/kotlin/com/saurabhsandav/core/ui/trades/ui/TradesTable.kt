package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeListItem
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    tradesItems: ImmutableList<TradeListItem>,
    onOpenDetails: (id: Long) -> Unit,
    onOpenChart: (id: Long) -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Quantity") { it.quantity }
        addColumnText("Avg. Entry") { it.entry }
        addColumnText("Avg. Exit") { it.exit ?: "NA" }
        addColumnText("Duration") { it.duration }
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

        tradesItems.forEach { tradeItem ->

            when (tradeItem) {
                is TradeListItem.DayHeader -> dayHeader(tradeItem)
                is TradeListItem.Entries -> tradeItems(
                    tradeItem = tradeItem,
                    onOpenDetails = onOpenDetails,
                    onOpenChart = onOpenChart,
                )
            }
        }
    }
}

private fun TableScope<TradeEntry>.dayHeader(tradeItem: TradeListItem.DayHeader) {

    stickyHeader {

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {

            Box(
                modifier = Modifier.fillParentMaxWidth().padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tradeItem.header)
            }
        }

        Divider()
    }
}

private fun TableScope<TradeEntry>.tradeItems(
    tradeItem: TradeListItem.Entries,
    onOpenDetails: (id: Long) -> Unit,
    onOpenChart: (id: Long) -> Unit,
) {

    rows(
        items = tradeItem.entries,
        key = { it.id },
    ) { item ->

        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem("Open Details") { onOpenDetails(item.id) },
                    ContextMenuItem("Open Chart") { onOpenChart(item.id) },
                )
            },
        ) {

            Column {

                DefaultTableRow(
                    item = item,
                    schema = schema,
                    onClick = { onOpenDetails(item.id) },
                )

                Divider()
            }
        }
    }
}
