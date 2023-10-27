package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.trade.model.TradeState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsTable(
    items: ImmutableList<TradeState.Execution>,
    newExecutionEnabled: Boolean,
    onAddToTrade: () -> Unit,
    onCloseTrade: () -> Unit,
    onNewFromExistingExecution: (Long) -> Unit,
    onEditExecution: (Long) -> Unit,
    onLockExecution: (Long) -> Unit,
    onDeleteExecution: (Long) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<TradeState.Execution> {
            addColumn(width = Fixed(48.dp)) { entry ->

                if (!entry.locked) {

                    TooltipArea(
                        tooltip = { Tooltip("Not locked") },
                        content = { Icon(Icons.Default.LockOpen, contentDescription = "Execution not locked") },
                    )
                }
            }
            addColumnText("Quantity") { it.quantity }
            addColumn("Side") {

                Text(
                    text = it.side,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
                )
            }
            addColumnText("Price") { it.price }
            addColumnText("Time", width = Weight(2.2F)) { it.timestamp }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            DefaultTableHeader(schema)

            Divider()

            items.forEach { item ->

                key(item) {

                    TradeExecutionItem(
                        schema = schema,
                        item = item,
                        onNewExecution = { onNewFromExistingExecution(item.id) },
                        onEditExecution = { onEditExecution(item.id) },
                        onLockExecution = { onLockExecution(item.id) },
                        onDeleteExecution = { onDeleteExecution(item.id) },
                    )
                }
            }

            if (newExecutionEnabled) {

                Row(modifier = Modifier.fillMaxWidth()) {

                    TextButton(
                        modifier = Modifier.weight(1F),
                        onClick = onAddToTrade,
                        shape = RectangleShape,
                        content = { Text("Add to Trade") },
                    )

                    TextButton(
                        modifier = Modifier.weight(1F),
                        onClick = onCloseTrade,
                        shape = RectangleShape,
                        content = { Text("Close Trade") },
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeExecutionItem(
    schema: TableSchema<TradeState.Execution>,
    item: TradeState.Execution,
    onNewExecution: () -> Unit,
    onEditExecution: () -> Unit,
    onLockExecution: () -> Unit,
    onDeleteExecution: () -> Unit,
) {

    var showLockConfirmationDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ContextMenuArea(
        items = {

            buildList {
                add(ContextMenuItem("New", onNewExecution))

                if (!item.locked) {
                    addAll(
                        listOf(
                            ContextMenuItem("Lock") { showLockConfirmationDialog = true },
                            ContextMenuItem("Edit", onEditExecution),
                            ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                        )
                    )
                }
            }
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
                confirmationRequestText = "Are you sure you want to lock the execution?",
                onDismiss = { showLockConfirmationDialog = false },
                onConfirm = {
                    showLockConfirmationDialog = false
                    onLockExecution()
                },
            )
        }

        if (showDeleteConfirmationDialog) {

            ConfirmationDialog(
                confirmationRequestText = "Are you sure you want to delete the execution?",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = onDeleteExecution,
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
