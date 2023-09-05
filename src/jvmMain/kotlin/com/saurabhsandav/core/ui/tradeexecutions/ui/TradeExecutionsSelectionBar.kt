package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ProfileTradeExecutionId
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry

@Composable
internal fun TradeExecutionsSelectionBar(
    selectionManager: SelectionManager<TradeExecutionEntry>,
    onLockExecutions: (List<ProfileTradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<ProfileTradeExecutionId>) -> Unit,
) {

    AnimatedVisibility(selectionManager.inMultiSelectMode) {

        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {

            Row(
                modifier = Modifier.height(48.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                var showLockConfirmationDialog by state { false }
                var showDeleteConfirmationDialog by state { false }

                IconButton(onClick = selectionManager::clear) {

                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }

                VerticalDivider()

                Text("${selectionManager.selection.size} item(s) selected")

                VerticalDivider()

                val allLocked by remember { derivedStateOf { selectionManager.selection.all { it.locked } } }

                AnimatedVisibility(!allLocked) {

                    TextButton(onClick = { showLockConfirmationDialog = true }) {

                        Text("LOCK")
                    }
                }

                VerticalDivider()

                TextButton(onClick = { showDeleteConfirmationDialog = true }) {

                    Text("DELETE")
                }

                if (showLockConfirmationDialog) {

                    ConfirmationDialog(
                        confirmationRequestText = "Are you sure you want to lock the executions?",
                        onDismiss = { showLockConfirmationDialog = false },
                        onConfirm = {
                            showLockConfirmationDialog = false
                            onLockExecutions(selectionManager.selection.map { it.profileTradeExecutionId })
                            selectionManager.clear()
                        },
                    )
                }

                if (showDeleteConfirmationDialog) {

                    ConfirmationDialog(
                        confirmationRequestText = "Are you sure you want to delete the executions?",
                        onDismiss = { showDeleteConfirmationDialog = false },
                        onConfirm = {
                            onDeleteExecutions(selectionManager.selection.map { it.profileTradeExecutionId })
                            selectionManager.clear()
                        },
                    )
                }
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
        modifier = Modifier.width(300.dp),
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
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(SnackbarDefaults.backgroundColor),
    )
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) = Canvas(modifier.fillMaxHeight().width(thickness)) {
    drawLine(
        color = color,
        strokeWidth = thickness.toPx(),
        start = Offset(thickness.toPx() / 2, 0f),
        end = Offset(thickness.toPx() / 2, size.height),
    )
}
