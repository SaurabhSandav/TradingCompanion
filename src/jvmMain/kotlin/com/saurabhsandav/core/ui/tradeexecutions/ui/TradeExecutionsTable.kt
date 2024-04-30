package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.common.table2.*
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ExecutionsList
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry

@Composable
internal fun TradeExecutionsTable(
    executionsList: ExecutionsList,
    isMarked: (TradeExecutionId) -> Boolean,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    LazyTable(
        headerContent = {

            TradeExecutionTableSchema.SimpleHeader {
                broker.text { "Broker" }
                ticker.text { "Ticker" }
                quantity.text { "Quantity" }
                side.text { "Side" }
                price.text { "Price" }
                time.text { "Time" }
            }
        },
    ) {

        executionRows(
            executions = executionsList.todayExecutions,
            title = "Today",
            isMarked = isMarked,
            onClickExecution = onClickExecution,
            onMarkExecution = onMarkExecution,
            onNewExecution = onNewExecution,
            onEditExecution = onEditExecution,
            onLockExecution = onLockExecution,
            onDeleteExecution = onDeleteExecution,
        )

        executionRows(
            executions = executionsList.pastExecutions,
            title = "Past",
            isMarked = isMarked,
            onClickExecution = onClickExecution,
            onMarkExecution = onMarkExecution,
            onNewExecution = onNewExecution,
            onEditExecution = onEditExecution,
            onLockExecution = onLockExecution,
            onDeleteExecution = onDeleteExecution,
        )
    }
}

private fun LazyListScope.executionRows(
    executions: List<TradeExecutionEntry>,
    title: String,
    isMarked: (TradeExecutionId) -> Boolean,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    if (executions.isNotEmpty()) {

        item(
            contentType = ContentType.Header,
        ) {

            ListItem(
                modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding),
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

            HorizontalDivider()
        }

        items(
            items = executions,
            key = { entry -> entry.id },
            contentType = { ContentType.Entry },
        ) { entry ->

            TradeExecutionEntry(
                entry = entry,
                isMarked = isMarked(entry.id),
                onMarkExecution = { onMarkExecution(entry.id) },
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
    entry: TradeExecutionEntry,
    isMarked: Boolean,
    onMarkExecution: () -> Unit,
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

            TradeExecutionTableSchema.SimpleRow(
                onClick = onClick,
                onLongClick = onLongClick,
            ) {
                selected {

                    Checkbox(
                        checked = isMarked,
                        onCheckedChange = { onMarkExecution() },
                    )
                }
                locked {

                    if (!entry.locked) {

                        TooltipArea(
                            tooltip = { Tooltip("Not locked") },
                            content = { Icon(Icons.Default.LockOpen, contentDescription = "Execution not locked") },
                        )
                    }
                }
                broker.text { entry.broker }
                ticker.text { entry.ticker }
                quantity.text { entry.quantity }
                side {

                    Text(
                        text = entry.side,
                        color = if (entry.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                price.text { entry.price }
                time.text { entry.timestamp }
            }

            HorizontalDivider()
        }

        if (showLockConfirmationDialog) {

            ConfirmationDialog(
                text = "Are you sure you want to lock the execution?",
                onDismiss = { showLockConfirmationDialog = false },
                onConfirm = {
                    showLockConfirmationDialog = false
                    onLockExecution()
                },
            )
        }

        if (showDeleteConfirmationDialog) {

            DeleteConfirmationDialog(
                subject = "execution",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = onDeleteExecution,
            )
        }
    }
}

private enum class ContentType {
    Header, Entry;
}

private object TradeExecutionTableSchema : TableSchema() {

    val selected = cell(Fixed(48.dp))
    val locked = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val quantity = cell()
    val side = cell()
    val price = cell()
    val time = cell(Weight(2.2F))
}
