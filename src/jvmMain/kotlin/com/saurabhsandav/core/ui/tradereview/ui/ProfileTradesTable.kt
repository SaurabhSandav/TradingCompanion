package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.saurabhsandav.core.thirdparty.paging_compose.collectAsLazyPagingItems
import com.saurabhsandav.core.thirdparty.paging_compose.itemKey
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Duration.Closed
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Duration.Open
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeItem
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ProfileTradesTable(
    trades: Flow<PagingData<TradeItem>>,
    onMarkTrade: (ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (ProfileTradeId) -> Unit,
    onOpenDetails: (ProfileTradeId) -> Unit,
    isFilterEnabled: Boolean,
    onFilter: () -> Unit,
) {

    val items = trades.collectAsLazyPagingItems()

    LazyTable(
        headerContent = {

            ProfileTradesTableSchema.SimpleHeader {
                mark.text { "Mark" }
                id.text { "ID" }
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

            OutlinedButton(
                modifier = Modifier.align(Alignment.End).padding(MaterialTheme.dimens.containerPadding),
                onClick = onFilter,
                shape = MaterialTheme.shapes.small,
                enabled = isFilterEnabled,
                content = { Text("Filter") },
            )

            HorizontalDivider()
        },
    ) {

        items(
            count = items.itemCount,
            key = items.itemKey { item -> item.profileTradeId },
        ) { index ->

            val item = items[index]!!

            TradeItem(
                item = item,
                onMarkTrade = { onMarkTrade(item.profileTradeId, it) },
                onSelectTrade = { onSelectTrade(item.profileTradeId) },
                onOpenDetails = { onOpenDetails(item.profileTradeId) },
            )
        }
    }
}

@Composable
private fun TradeItem(
    item: TradeItem,
    onMarkTrade: (Boolean) -> Unit,
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

            ProfileTradesTableSchema.SimpleRow(
                onClick = onSelectTrade,
            ) {
                mark {

                    Checkbox(
                        checked = item.isMarked,
                        onCheckedChange = { onMarkTrade(it) }
                    )
                }
                id.text { item.profileTradeId.tradeId.toString() }
                broker.text { item.broker }
                ticker.text { item.ticker }
                side {
                    Text(item.side, color = if (item.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { item.quantity }
                avgEntry.text { item.entry }
                avgExit.text { item.exit ?: "NA" }
                entryTime.text { item.entryTime }
                duration {

                    Text(
                        text = when (val duration = item.duration) {
                            is Open -> duration.flow.collectAsState("").value
                            is Closed -> duration.str
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl {
                    Text(item.pnl, color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl {
                    Text(item.netPnl, color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
            }

            HorizontalDivider()
        }
    }
}

private object ProfileTradesTableSchema : TableSchema() {

    val mark = cell(Fixed(48.dp))
    val id = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val entryTime = cell(Weight(2.2F))
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
}
