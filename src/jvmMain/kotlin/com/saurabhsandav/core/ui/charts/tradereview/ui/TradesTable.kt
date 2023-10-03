package com.saurabhsandav.core.ui.charts.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradesByDay
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    tradesByDays: ImmutableList<TradesByDay>,
    onMarkTrade: (id: Long, isMarked: Boolean) -> Unit,
    onSelectTrade: (id: Long) -> Unit,
    onOpenDetails: (id: Long) -> Unit,
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

        tradesByDays.forEach { tradesByDay ->

            stickyHeader {

                DayHeader(
                    modifier = Modifier.fillParentMaxWidth(),
                    header = tradesByDay.dayHeader,
                )
            }

            rows(
                items = tradesByDay.trades,
                key = { it.id },
            ) { entry ->

                TradeEntry(
                    schema = schema,
                    entry = entry,
                    onSelectTrade = { onSelectTrade(entry.id) },
                    onOpenDetails = { onOpenDetails(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    modifier: Modifier,
    header: String,
) {

    Column {

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {

            Box(
                modifier = modifier.padding(8.dp),
                contentAlignment = Alignment.Center,
                content = { Text(header) },
            )

            Divider()
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

            Divider()
        }
    }
}
