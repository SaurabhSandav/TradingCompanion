package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table2.*
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Weight
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeEntry

@Composable
internal fun MarkedTradesTable(
    markedTrades: List<MarkedTradeEntry>,
    onUnMarkTrade: (ProfileTradeId) -> Unit,
    onSelectTrade: (ProfileTradeId) -> Unit,
    onOpenDetails: (ProfileTradeId) -> Unit,
) {

    LazyTable(
        headerContent = {

            MarkedTradesTableSchema.SimpleHeader {
                mark.text { "Mark" }
                id.text { "ID" }
                profile.text { "Profile" }
                broker.text { "Broker" }
                ticker.text { "Ticker" }
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
            items = markedTrades,
            key = { it.profileTradeId },
        ) { entry ->

            MarkedTradeEntry(
                entry = entry,
                onUnMarkTrade = { onUnMarkTrade(entry.profileTradeId) },
                onSelectTrade = { onSelectTrade(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun MarkedTradeEntry(
    entry: MarkedTradeEntry,
    onUnMarkTrade: () -> Unit,
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

            MarkedTradesTableSchema.SimpleRow(
                onClick = onSelectTrade,
            ) {
                mark {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onUnMarkTrade() }
                    )
                }
                id.text { entry.profileTradeId.tradeId.toString() }
                profile.text { entry.profileName }
                broker.text { entry.broker }
                ticker.text { entry.ticker }
                side {
                    Text(entry.side, color = if (entry.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { entry.quantity }
                avgEntry.text { entry.entry }
                avgExit.text { entry.exit ?: "NA" }
                entryTime.text { entry.entryTime }
                duration {

                    Text(
                        text = entry.duration.collectAsState("").value,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl {
                    Text(entry.pnl, color = if (entry.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl {
                    Text(entry.netPnl, color = if (entry.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
            }

            HorizontalDivider()
        }
    }
}

private object MarkedTradesTableSchema : TableSchema() {

    val mark = cell(Fixed(48.dp))
    val id = cell(Fixed(48.dp))
    val profile = cell(Weight(1.3F))
    val broker = cell(Weight(1.3F))
    val ticker = cell(Weight(1.3F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val entryTime = cell(Weight(2.2F))
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
}
