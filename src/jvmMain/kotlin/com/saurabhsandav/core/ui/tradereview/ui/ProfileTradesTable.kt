package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table2.*
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeEntry

@Composable
internal fun ProfileTradesTable(
    trades: List<TradeEntry>,
    onMarkTrade: (profileTradeId: ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
    isFilterEnabled: Boolean,
    onFilter: () -> Unit,
) {

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
            items = trades,
            key = { it.profileTradeId },
        ) { entry ->

            TradeEntry(
                entry = entry,
                onMarkTrade = { onMarkTrade(entry.profileTradeId, it) },
                onSelectTrade = { onSelectTrade(entry.profileTradeId) },
                onOpenDetails = { onOpenDetails(entry.profileTradeId) },
            )
        }
    }
}

@Composable
private fun TradeEntry(
    entry: TradeEntry,
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
                        checked = entry.isMarked,
                        onCheckedChange = { onMarkTrade(it) }
                    )
                }
                id.text { entry.profileTradeId.tradeId.toString() }
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
