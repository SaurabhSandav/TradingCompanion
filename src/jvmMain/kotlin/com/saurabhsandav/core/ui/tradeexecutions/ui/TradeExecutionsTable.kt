package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ProfileTradeExecutionId
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsTable(
    executions: ImmutableList<TradeExecutionEntry>,
    isMarked: (TradeExecutionEntry) -> Boolean,
    onClickExecution: (TradeExecutionEntry) -> Unit,
    onMarkExecution: (TradeExecutionEntry) -> Unit,
    onNewExecution: (ProfileTradeExecutionId) -> Unit,
    onEditExecution: (ProfileTradeExecutionId) -> Unit,
    onLockExecution: (ProfileTradeExecutionId) -> Unit,
    onDeleteExecution: (ProfileTradeExecutionId) -> Unit,
) {

    val schema = rememberTableSchema<TradeExecutionEntry> {
        addColumn(width = Weight(.5F)) { entry ->

            Checkbox(
                checked = isMarked(entry),
                onCheckedChange = { onMarkExecution(entry) },
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

        rows(
            items = executions,
            key = { it.profileTradeExecutionId },
        ) { entry ->

            TradeExecutionEntry(
                schema = schema,
                entry = entry,
                onClick = { onClickExecution(entry) },
                onLongClick = { onMarkExecution(entry) },
                onNewExecution = { onNewExecution(entry.profileTradeExecutionId) },
                onEditExecution = { onEditExecution(entry.profileTradeExecutionId) },
                onLockExecution = { onLockExecution(entry.profileTradeExecutionId) },
                onDeleteExecution = { onDeleteExecution(entry.profileTradeExecutionId) },
            )
        }
    }
}

@Composable
private fun TradeExecutionEntry(
    schema: TableSchema<TradeExecutionEntry>,
    entry: TradeExecutionEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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

                if (!entry.locked) {
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
                item = entry,
                schema = schema,
                onClick = onClick,
                onLongClick = onLongClick,
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
