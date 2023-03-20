package com.saurabhsandav.core.ui.charts.tradereview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeListItem
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    tradesItems: ImmutableList<TradeListItem>,
    onMarkTrade: (id: Long, isMarked: Boolean) -> Unit,
    onSelectTrade: (id: Long) -> Unit,
) {

    val schema = rememberTableSchema<TradeEntry> {
        addColumn("Mark") { tradeEntry ->
            Checkbox(
                checked = tradeEntry.isMarked,
                onCheckedChange = { onMarkTrade(tradeEntry.id, it) }
            )
        }
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
    }

    LazyTable(
        schema = schema,
    ) {

        tradesItems.forEach { tradeItem ->

            when (tradeItem) {
                is TradeListItem.DayHeader -> dayHeader(tradeItem)
                is TradeListItem.Entries -> tradeItems(
                    tradeItem = tradeItem,
                    onSelectTrade = onSelectTrade,
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
    onSelectTrade: (id: Long) -> Unit,
) {

    rows(
        items = tradeItem.entries,
        key = { it.id },
    ) { item ->

        Column {

            DefaultTableRow(
                modifier = Modifier.clickable { onSelectTrade(item.id) },
                item = item,
                schema = schema,
            )

            Divider()
        }
    }
}
