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
import com.saurabhsandav.core.ui.trades.model.TradesState.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesTable(
    tradesByDays: ImmutableList<TradesByDay>,
    onOpenDetails: (ProfileTradeId) -> Unit,
    onOpenChart: (ProfileTradeId) -> Unit,
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

        tradesByDays.forEach { tradesByDay ->

            stickyHeader {

                DayHeader(
                    modifier = Modifier.fillParentMaxWidth(),
                    header = tradesByDay.dayHeader,
                )
            }

            rows(
                items = tradesByDay.trades,
                key = { it.profileTradeId },
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
