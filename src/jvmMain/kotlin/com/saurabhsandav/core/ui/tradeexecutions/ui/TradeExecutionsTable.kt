package com.saurabhsandav.core.ui.tradeexecutions.ui

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
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsTable(
    items: ImmutableList<TradeExecutionListItem>,
    isMarked: (TradeExecutionEntry) -> Boolean,
    onClickExecution: (TradeExecutionEntry) -> Unit,
    onMarkExecution: (TradeExecutionEntry) -> Unit,
    onNewExecution: (ProfileTradeExecutionId) -> Unit,
    onEditExecution: (ProfileTradeExecutionId) -> Unit,
    onLockExecution: (ProfileTradeExecutionId) -> Unit,
    onDeleteExecution: (ProfileTradeExecutionId) -> Unit,
) {

    val schema = rememberTableSchema<TradeExecutionEntry> {
        addColumn(span = .5F) { entry ->

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

        items.forEach { item ->

            when (item) {
                is TradeExecutionListItem.DayHeader -> dayHeader(item)
                is TradeExecutionListItem.Entries -> tradeExecutionItems(
                    tradeExecutionListItem = item,
                    onClickExecution = onClickExecution,
                    onLongClickExecution = onMarkExecution,
                    onNewExecution = onNewExecution,
                    onEditExecution = onEditExecution,
                    onLockExecution = onLockExecution,
                    onDeleteExecution = onDeleteExecution,
                )
            }
        }
    }
}

private fun TableScope<TradeExecutionEntry>.dayHeader(item: TradeExecutionListItem.DayHeader) {

    stickyHeader {

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {

            Box(
                modifier = Modifier.fillParentMaxWidth().padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.header)
            }
        }

        Divider()
    }
}

private fun TableScope<TradeExecutionEntry>.tradeExecutionItems(
    tradeExecutionListItem: TradeExecutionListItem.Entries,
    onClickExecution: (TradeExecutionEntry) -> Unit,
    onLongClickExecution: (TradeExecutionEntry) -> Unit,
    onNewExecution: (ProfileTradeExecutionId) -> Unit,
    onEditExecution: (ProfileTradeExecutionId) -> Unit,
    onLockExecution: (ProfileTradeExecutionId) -> Unit,
    onDeleteExecution: (ProfileTradeExecutionId) -> Unit,
) {

    rows(
        items = tradeExecutionListItem.entries,
        key = { it.profileTradeExecutionId },
    ) { entry ->

        var showLockConfirmationDialog by state { false }
        var showDeleteConfirmationDialog by state { false }

        ContextMenuArea(
            items = {

                buildList {
                    add(ContextMenuItem("New") { onNewExecution(entry.profileTradeExecutionId) })

                    if (!entry.locked) {
                        addAll(
                            listOf(
                                ContextMenuItem("Lock") { showLockConfirmationDialog = true },
                                ContextMenuItem("Edit") { onEditExecution(entry.profileTradeExecutionId) },
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
                    onClick = { onClickExecution(entry) },
                    onLongClick = { onLongClickExecution(entry) },
                )

                Divider()
            }

            if (showLockConfirmationDialog) {

                ConfirmationDialog(
                    confirmationRequestText = "Are you sure you want to lock the execution?",
                    onDismiss = { showLockConfirmationDialog = false },
                    onConfirm = {
                        showLockConfirmationDialog = false
                        onLockExecution(entry.profileTradeExecutionId)
                    },
                )
            }

            if (showDeleteConfirmationDialog) {

                ConfirmationDialog(
                    confirmationRequestText = "Are you sure you want to delete the execution?",
                    onDismiss = { showDeleteConfirmationDialog = false },
                    onConfirm = { onDeleteExecution(entry.profileTradeExecutionId) },
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
