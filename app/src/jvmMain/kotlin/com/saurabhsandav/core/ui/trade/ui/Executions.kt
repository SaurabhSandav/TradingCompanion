package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState

@Composable
internal fun ExecutionsTable(
    items: List<TradeState.Execution>,
    newExecutionEnabled: Boolean,
    onAddToTrade: () -> Unit,
    onCloseTrade: () -> Unit,
    onNewFromExistingExecution: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
    modifier: Modifier = Modifier,
) {

    TradeSection(
        modifier = modifier,
        title = "Executions",
        subtitle = when {
            items.isEmpty() -> "No Executions"
            items.size == 1 -> "1 Execution"
            else -> "${items.size} Executions"
        },
        trailingContent = {

            AnimatedVisibility(
                visible = newExecutionEnabled,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                ) {

                    TradeSectionButton(
                        onClick = onAddToTrade,
                        text = "Add To Trade",
                    )

                    TradeSectionButton(
                        onClick = onCloseTrade,
                        text = "Close Trade",
                        icon = {

                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Close Trade",
                            )
                        },
                    )
                }
            }
        },
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            TradeExecutionTableSchema.SimpleHeader {
                quantity.text { "Quantity" }
                side.text { "Side" }
                price.text { "Price" }
                time.text { "Time" }
            }

            items.forEach { item ->

                key(item) {

                    TradeExecutionItem(
                        item = item,
                        onNewExecution = { onNewFromExistingExecution(item.id) },
                        onEditExecution = { onEditExecution(item.id) },
                        onLockExecution = { onLockExecution(item.id) },
                        onDeleteExecution = { onDeleteExecution(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeExecutionItem(
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
                        ),
                    )
                }
            }
        },
    ) {

        Column {

            TradeExecutionTableSchema.SimpleRow {
                locked {

                    if (!item.locked) {

                        SimpleTooltipBox("Not locked") {
                            Icon(Icons.Default.LockOpen, contentDescription = "Execution not locked")
                        }
                    }
                }
                quantity.text { item.quantity }
                side {

                    Text(
                        text = item.side,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (item.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                price.text { item.price }
                time.text { item.timestamp }
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

private object TradeExecutionTableSchema : TableSchema() {

    val locked = cell(Fixed(48.dp))
    val quantity = cell()
    val side = cell()
    val price = cell()
    val time = cell(Weight(2.2F))
}
