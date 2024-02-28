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
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Fixed
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.trade.model.TradeState

@Composable
internal fun TradeExecutionsTable(
    items: List<TradeState.Execution>,
    newExecutionEnabled: Boolean,
    onAddToTrade: () -> Unit,
    onCloseTrade: () -> Unit,
    onNewFromExistingExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
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

            HorizontalDivider()

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
