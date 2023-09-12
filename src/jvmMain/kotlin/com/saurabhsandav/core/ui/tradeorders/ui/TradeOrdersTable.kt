package com.saurabhsandav.core.ui.tradeorders.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeOrdersTable(
    tradeOrderItems: ImmutableList<TradeOrderListItem>,
    isMarked: (TradeOrderEntry) -> Boolean,
    onClickOrder: (TradeOrderEntry) -> Unit,
    onMarkOrder: (TradeOrderEntry) -> Unit,
    onNewOrder: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrder: (ProfileOrderId) -> Unit,
    onDeleteOrder: (ProfileOrderId) -> Unit,
) {

    val schema = rememberTableSchema<TradeOrderEntry> {
        addColumn(span = .5F) { entry ->

            Checkbox(
                checked = isMarked(entry),
                onCheckedChange = { onMarkOrder(entry) },
            )
        }
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") { entry ->

            Text(
                text = entry.side,
                color = if (entry.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
            )
        }
        addColumnText("Price") { it.price }
        addColumnText("Time") { it.timestamp }
    }

    LazyTable(
        schema = schema,
    ) {

        tradeOrderItems.forEach { tradeOrderListItem ->

            when (tradeOrderListItem) {
                is TradeOrderListItem.DayHeader -> dayHeader(tradeOrderListItem)
                is TradeOrderListItem.Entries -> tradeOrderItems(
                    tradeOrderListItem = tradeOrderListItem,
                    onClickOrder = onClickOrder,
                    onLongClickOrder = onMarkOrder,
                    onNewOrder = onNewOrder,
                    onEditOrder = onEditOrder,
                    onLockOrder = onLockOrder,
                    onDeleteOrder = onDeleteOrder,
                )
            }
        }
    }
}

private fun TableScope<TradeOrderEntry>.dayHeader(tradeOrderListItem: TradeOrderListItem.DayHeader) {

    stickyHeader {

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {

            Box(
                modifier = Modifier.fillParentMaxWidth().padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(tradeOrderListItem.header)
            }
        }

        Divider()
    }
}

private fun TableScope<TradeOrderEntry>.tradeOrderItems(
    tradeOrderListItem: TradeOrderListItem.Entries,
    onClickOrder: (TradeOrderEntry) -> Unit,
    onLongClickOrder: (TradeOrderEntry) -> Unit,
    onNewOrder: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrder: (ProfileOrderId) -> Unit,
    onDeleteOrder: (ProfileOrderId) -> Unit,
) {

    rows(
        items = tradeOrderListItem.entries,
        key = { it.profileOrderId },
    ) { entry ->

        var showLockConfirmationDialog by state { false }
        var showDeleteConfirmationDialog by state { false }

        ContextMenuArea(
            items = {

                buildList {
                    add(ContextMenuItem("New") { onNewOrder(entry.profileOrderId) })

                    if (!entry.locked) {
                        addAll(
                            listOf(
                                ContextMenuItem("Lock") { showLockConfirmationDialog = true },
                                ContextMenuItem("Edit") { onEditOrder(entry.profileOrderId) },
                                ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                            )
                        )
                    }
                }
            },
        ) {

            Column {

                DefaultTableRow(
                    item = entry,
                    schema = schema,
                    onClick = { onClickOrder(entry) },
                    onLongClick = { onLongClickOrder(entry) },
                )

                Divider()
            }

            if (showLockConfirmationDialog) {

                ConfirmationDialog(
                    confirmationRequestText = "Are you sure you want to lock the order?",
                    onDismiss = { showLockConfirmationDialog = false },
                    onConfirm = {
                        showLockConfirmationDialog = false
                        onLockOrder(entry.profileOrderId)
                    },
                )
            }

            if (showDeleteConfirmationDialog) {

                ConfirmationDialog(
                    confirmationRequestText = "Are you sure you want to delete the order?",
                    onDismiss = { showDeleteConfirmationDialog = false },
                    onConfirm = { onDeleteOrder(entry.profileOrderId) },
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    confirmationRequestText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text(confirmationRequestText)
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}
