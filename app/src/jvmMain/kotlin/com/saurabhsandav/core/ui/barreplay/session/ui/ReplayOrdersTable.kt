package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trading.backtest.BacktestOrderId
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayOrderListItem
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.text

@Composable
internal fun ReplayOrdersTable(
    replayOrderItems: List<ReplayOrderListItem>,
    onCancelOrder: (BacktestOrderId) -> Unit,
) {

    LazyTable(
        headerContent = {

            ReplayOrderTableSchema.SimpleHeader {
                executionType.text { "Execution Type" }
                broker.text { "Broker" }
                ticker.text { "Ticker" }
                quantity.text { "Quantity" }
                side.text { "Side" }
                price.text { "Price" }
                time.text { "Time" }
            }
        },
    ) {

        items(
            items = replayOrderItems,
            key = { it.id },
        ) { item ->

            var showCancelConfirmationDialog by state { false }

            Column(Modifier.animateItem()) {

                ContextMenuArea(
                    items = {

                        buildList {
                            addAll(
                                listOf(
                                    ContextMenuItem("Cancel") { showCancelConfirmationDialog = true },
                                ),
                            )
                        }
                    },
                ) {

                    ReplayOrderTableSchema.SimpleRow {
                        executionType.text { item.executionType }
                        broker.text { item.broker }
                        ticker.text { item.ticker }
                        quantity.text { item.quantity }
                        side {
                            Text(item.side, color = if (item.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed)
                        }
                        price.text { item.price }
                        time.text { item.timestamp }
                    }
                }

                HorizontalDivider()
            }

            if (showCancelConfirmationDialog) {

                ConfirmationDialog(
                    text = "Are you sure you want to cancel the order?",
                    onDismiss = { showCancelConfirmationDialog = false },
                    onConfirm = { onCancelOrder(item.id) },
                )
            }
        }
    }
}

private object ReplayOrderTableSchema : TableSchema() {

    val executionType = cell()
    val broker = cell()
    val ticker = cell()
    val quantity = cell()
    val side = cell()
    val price = cell()
    val time = cell()
}
