package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsTable(
    todayExecutions: ImmutableList<TradeExecutionEntry>,
    pastExecutions: ImmutableList<TradeExecutionEntry>,
    isMarked: (TradeExecutionId) -> Boolean,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    val schema = rememberTableSchema<TradeExecutionEntry> {
        addColumn(width = Fixed(48.dp)) { entry ->

            Checkbox(
                checked = isMarked(entry.id),
                onCheckedChange = { onMarkExecution(entry.id) },
            )
        }
        addColumn(width = Fixed(48.dp)) { entry ->

            if (!entry.locked) {

                TooltipArea(
                    tooltip = { Tooltip("Not locked") },
                    content = { Icon(Icons.Default.LockOpen, contentDescription = "Execution not locked") },
                )
            }
        }
        addColumnText("Broker", width = Weight(2F)) { it.broker }
        addColumnText("Ticker", width = Weight(1.7F)) { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") { entry ->

            Text(
                text = entry.side,
                color = if (entry.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
            )
        }
        addColumnText("Price") { it.price }
        addColumnText("Time", width = Weight(2.2F)) { it.timestamp }
    }

    LazyTable(
        schema = schema,
    ) {

        executionRows(
            executions = todayExecutions,
            title = "Today",
            onClickExecution = onClickExecution,
            onMarkExecution = onMarkExecution,
            onNewExecution = onNewExecution,
            onEditExecution = onEditExecution,
            onLockExecution = onLockExecution,
            onDeleteExecution = onDeleteExecution,
        )

        executionRows(
            executions = pastExecutions,
            title = "Past",
            onClickExecution = onClickExecution,
            onMarkExecution = onMarkExecution,
            onNewExecution = onNewExecution,
            onEditExecution = onEditExecution,
            onLockExecution = onLockExecution,
            onDeleteExecution = onDeleteExecution,
        )
    }
}

private fun TableScope<TradeExecutionEntry>.executionRows(
    executions: ImmutableList<TradeExecutionEntry>,
    title: String,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    if (executions.isNotEmpty()) {

        row(
            contentType = ContentType.Header,
        ) {

            ListItem(
                modifier = Modifier.padding(16.dp),
                headlineContent = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = "${executions.size} Executions",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )

            Divider()
        }

        rows(
            items = executions,
            key = { entry -> entry.id },
            contentType = { ContentType.Entry },
        ) { entry ->

            TradeExecutionEntry(
                schema = schema,
                entry = entry,
                onClick = { onClickExecution(entry.id) },
                onLongClick = { onMarkExecution(entry.id) },
                onNewExecution = { onNewExecution(entry.id) },
                onEditExecution = { onEditExecution(entry.id) },
                onLockExecution = { onLockExecution(entry.id) },
                onDeleteExecution = { onDeleteExecution(entry.id) },
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

private enum class ContentType {
    Header, Entry;
}
