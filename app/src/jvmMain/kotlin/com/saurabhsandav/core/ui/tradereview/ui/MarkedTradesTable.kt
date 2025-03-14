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
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Duration.Closed
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Duration.Open
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeItem

@Composable
internal fun MarkedTradesTable(
    markedTrades: List<MarkedTradeItem>?,
    onUnMarkTrade: (ProfileTradeId) -> Unit,
    onSelectTrade: (ProfileTradeId) -> Unit,
    onOpenDetails: (ProfileTradeId) -> Unit,
) {

    ListLoadStateIndicator(
        state = {
            when {
                markedTrades == null -> loading()
                markedTrades.isEmpty() -> empty()
                else -> loaded()
            }
        },
        emptyText = { "No Trades" },
    ) {

        MarkedTradesTable(
            markedTrades = markedTrades ?: emptyList(),
            onUnMarkTrade = onUnMarkTrade,
            onSelectTrade = onSelectTrade,
            onOpenDetails = onOpenDetails,
        )
    }
}

@JvmName("MarkedTradesTableActual")
@Composable
private fun MarkedTradesTable(
    markedTrades: List<MarkedTradeItem>,
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
        ) { item ->

            MarkedTradeItem(
                modifier = Modifier.animateItem(),
                item = item,
                onUnMarkTrade = { onUnMarkTrade(item.profileTradeId) },
                onSelectTrade = { onSelectTrade(item.profileTradeId) },
                onOpenDetails = { onOpenDetails(item.profileTradeId) },
            )
        }
    }
}

@Composable
private fun MarkedTradeItem(
    modifier: Modifier,
    item: MarkedTradeItem,
    onUnMarkTrade: () -> Unit,
    onSelectTrade: () -> Unit,
    onOpenDetails: () -> Unit,
) {

    // Passing the animateItem() modifier to ListItem doesn't work.
    // Use Box to workaround as the ContextMenuArea doesn't have a modifier parameter.
    Column(modifier) {

        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem("Open Details", onOpenDetails),
                )
            },
        ) {

            MarkedTradesTableSchema.SimpleRow(
                onClick = onSelectTrade,
            ) {
                mark.content {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onUnMarkTrade() },
                    )
                }
                id.text { item.profileTradeId.tradeId.toString() }
                profile.text { item.profileName }
                broker.text { item.broker }
                ticker.text { item.ticker }
                side.content {
                    Text(item.side, color = if (item.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { item.quantity }
                avgEntry.text { item.entry }
                avgExit.text { item.exit ?: "NA" }
                entryTime.text { item.entryTime }
                duration.content {

                    Text(
                        text = when (val duration = item.duration) {
                            is Open -> duration.flow.collectAsState("").value
                            is Closed -> duration.str
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl.content {
                    Text(item.pnl, color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl.content {
                    Text(item.netPnl, color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
            }
        }

        HorizontalDivider()
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
