package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.trade.model.TradeState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeOrdersTable(
    tradeOrderItems: ImmutableList<TradeState.Order>,
    onEditOrder: (Long) -> Unit,
    onLockOrder: (Long) -> Unit,
    onDeleteOrder: (Long) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<TradeState.Order> {
            addColumnText("Quantity") { it.quantity }
            addColumn("Type") {

                Text(
                    text = it.type,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it.type == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
                )
            }
            addColumnText("Price") { it.price }
            addColumnText("Time") { it.timestamp }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            DefaultTableHeader(schema)

            Divider()

            tradeOrderItems.forEach { item ->

                key(item) {

                    TradeOrderItem(
                        schema = schema,
                        item = item,
                        onEditOrder = { onEditOrder(item.id) },
                        onLockOrder = { onLockOrder(item.id) },
                        onDeleteOrder = { onDeleteOrder(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeOrderItem(
    schema: TableSchema<TradeState.Order>,
    item: TradeState.Order,
    onEditOrder: () -> Unit,
    onLockOrder: () -> Unit,
    onDeleteOrder: () -> Unit,
) {

    var showLockConfirmationDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ContextMenuArea(
        items = {

            if (!item.locked) {
                listOf(
                    ContextMenuItem("Lock") { showLockConfirmationDialog = true },
                    ContextMenuItem("Edit", onEditOrder),
                    ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                )
            } else emptyList()
        },
    ) {

        Column {

            DefaultTableRow(
                item = item,
                schema = schema,
            )

            Divider()
        }

        if (showLockConfirmationDialog) {

            ConfirmationDialog(
                confirmationRequestText = "Are you sure you want to lock the order?",
                onDismiss = { showLockConfirmationDialog = false },
                onConfirm = {
                    showLockConfirmationDialog = false
                    onLockOrder()
                },
            )
        }

        if (showDeleteConfirmationDialog) {

            ConfirmationDialog(
                confirmationRequestText = "Are you sure you want to delete the order?",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = onDeleteOrder,
            )
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
