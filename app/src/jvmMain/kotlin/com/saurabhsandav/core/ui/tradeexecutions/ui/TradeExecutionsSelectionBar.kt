package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.*
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun TradeExecutionsSelectionBar(
    selectionManager: SelectionManager<TradeExecutionId>,
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
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

                TextButton(onClick = { showLockConfirmationDialog = true }) {

                    Text("LOCK")
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
