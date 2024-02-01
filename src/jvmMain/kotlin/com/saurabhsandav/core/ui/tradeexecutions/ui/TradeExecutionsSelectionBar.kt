package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.*

@Composable
internal fun TradeExecutionsSelectionBar(
    selectionManager: SelectionManager<TradeExecutionId>,
    canSelectionLock: Boolean,
    onLockExecutions: (List<TradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<TradeExecutionId>) -> Unit,
) {

    AnimatedVisibility(
        visible = selectionManager.selection.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {

        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {

            Row(
                modifier = Modifier.height(48.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                var showLockConfirmationDialog by state { false }
                var showDeleteConfirmationDialog by state { false }

                IconButtonWithTooltip(
                    onClick = selectionManager::clear,
                    tooltipText = "Clear selection",
                    content = {
                        Icon(Icons.Default.Close, contentDescription = "Clear selection")
                    },
                )

                VerticalDivider()

                Text("${selectionManager.selection.size} item(s) selected")

                VerticalDivider()

                AnimatedVisibility(canSelectionLock) {

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
                        text = "Are you sure you want to lock the executions?",
                        onDismiss = { showLockConfirmationDialog = false },
                        onConfirm = {
                            showLockConfirmationDialog = false
                            onLockExecutions(selectionManager.selection.toList())
                            selectionManager.clear()
                        },
                    )
                }

                if (showDeleteConfirmationDialog) {

                    DeleteConfirmationDialog(
                        subject = "executions",
                        onDismiss = { showDeleteConfirmationDialog = false },
                        onConfirm = {
                            onDeleteExecutions(selectionManager.selection.toList())
                            selectionManager.clear()
                        },
                    )
                }
            }
        }
    }
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
