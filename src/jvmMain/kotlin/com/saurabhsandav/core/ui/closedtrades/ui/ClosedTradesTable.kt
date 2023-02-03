package com.saurabhsandav.core.ui.closedtrades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.TooltipArea
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
import com.saurabhsandav.core.ui.closedtrades.model.ClosedTradeListItem
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.table.*

@Composable
internal fun ClosedTradesTable(
    closedTradesItems: Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>,
    onOpenChart: (id: Long) -> Unit,
    onEditTrade: (id: Long) -> Unit,
    onOpenPNLCalculator: (id: Long) -> Unit,
    onDeleteTrade: (id: Long) -> Unit,
) {

    val schema = rememberTableSchema<ClosedTradeListItem.Entry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Entry") { it.entry }
        addColumnText("Exit") { it.exit }
        addColumnText("Stop") { it.stop }
        addColumnText("Target") { it.target }
        addColumnText("Duration") { it.duration }
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Favorable Excursion") },
                    content = { Text("MFE") },
                )
            },
            content = { Text(it.maxFavorableExcursion) }
        )
        addColumn(
            header = {

                TooltipArea(
                    tooltip = { Tooltip("Maximum Adverse Excursion") },
                    content = { Text("MAE") },
                )
            },
            content = { Text(it.maxAdverseExcursion) }
        )
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

        closedTradesItems.forEach { (dayHeader, entries) ->

            stickyHeader {

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {

                    Box(
                        modifier = Modifier.fillParentMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dayHeader.header)
                    }
                }

                Divider()
            }

            rows(
                items = entries,
                key = { it.id },
            ) { item ->

                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Open Chart") { onOpenChart(item.id) },
                            ContextMenuItem("Edit") { onEditTrade(item.id) },
                            ContextMenuItem("PNL Calculator") { onOpenPNLCalculator(item.id) },
                            ContextMenuItem("Delete") { onDeleteTrade(item.id) },
                        )
                    },
                ) {

                    Column {

                        DefaultTableRow(item, schema)

                        Divider()
                    }
                }
            }
        }
    }
}
