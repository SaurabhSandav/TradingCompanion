package com.saurabhsandav.core.ui.review.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry.Duration.Closed
import com.saurabhsandav.core.ui.review.model.ReviewState.TradeEntry.Duration.Open
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

@Composable
internal fun TradesTable(
    trades: List<TradeEntry>,
    onOpenChart: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
) {

    LazyTable(
        headerContent = {

            TradesTableSchema.SimpleHeader {
                id.text { "ID" }
                broker.text { "Broker" }
                symbol.text { "Symbol" }
                side.text { "Side" }
                quantity.text { "Quantity" }
                avgEntry.text { "Avg. Entry" }
                avgExit.text { "Avg. Exit" }
                entryTime.text { "Entry Time" }
                duration.text { "Duration" }
                pnl.text { "PNL" }
                netPnl.text { "Net PNL" }
            }
        },
    ) {

        items(
            items = trades,
            key = { it.profileTradeId },
        ) { entry ->

            TradeEntry(
                entry = entry,
                onOpenChart = { onOpenChart(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
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

            TradesTableSchema.SimpleRow {
                id.text { entry.profileTradeId.tradeId.toString() }
                broker.text { entry.broker }
                symbol.text { entry.ticker }
                side.content {
                    Text(entry.side, color = if (entry.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { entry.quantity }
                avgEntry.text { entry.entry }
                avgExit.text { entry.exit ?: "NA" }
                entryTime.text { entry.entryTime }
                duration.content {

                    Text(
                        text = when (val duration = entry.duration) {
                            is Open -> duration.flow.collectAsState("").value
                            is Closed -> duration.str
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl.content {
                    Text(entry.pnl, color = if (entry.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl.content {
                    Text(entry.netPnl, color = if (entry.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
            }

            HorizontalDivider()
        }
    }
}

private object TradesTableSchema : TableSchema() {

    val id = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val symbol = cell(Weight(1.7F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val entryTime = cell(Weight(2.2F))
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
}
