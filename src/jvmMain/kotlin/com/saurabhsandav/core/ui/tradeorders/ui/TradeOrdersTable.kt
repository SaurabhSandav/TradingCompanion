package com.saurabhsandav.core.ui.tradeorders.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.SnackbarDefaults
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
    onNewOrder: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrder: (ProfileOrderId) -> Unit,
    onDeleteOrder: (ProfileOrderId) -> Unit,
) {

    val schema = rememberTableSchema<TradeOrderEntry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Type") {
            Text(it.type, color = if (it.type == "BUY") AppColor.ProfitGreen else AppColor.LossRed)
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
    onNewOrder: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrder: (ProfileOrderId) -> Unit,
    onDeleteOrder: (ProfileOrderId) -> Unit,
) {

    rows(
        items = tradeOrderListItem.entries,
        key = { it.profileOrderId },
    ) { item ->

        var showDeleteConfirmationDialog by state { false }

        ContextMenuArea(
            items = {

                buildList {
                    add(ContextMenuItem("New") { onNewOrder(item.profileOrderId) })

                    if (!item.locked) {
                        addAll(
                            listOf(
                                ContextMenuItem("Lock") { onLockOrder(item.profileOrderId) },
                                ContextMenuItem("Edit") { onEditOrder(item.profileOrderId) },
                                ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                            )
                        )
                    }
                }
            },
        ) {

            Column {

                DefaultTableRow(item, schema)

                Divider()
            }

            if (showDeleteConfirmationDialog) {

                DeleteConfirmationDialog(
                    onDismiss = { showDeleteConfirmationDialog = false },
                    onConfirm = { onDeleteOrder(item.profileOrderId) },
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        modifier = Modifier.width(300.dp),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the order?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(SnackbarDefaults.backgroundColor),
    )
}
