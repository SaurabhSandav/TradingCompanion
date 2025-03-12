package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.SelectionBar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun TradeExecutionsSelectionBar(
    selectionManager: SelectionManager<TradeExecutionId>,
    onLockExecutions: (List<TradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<TradeExecutionId>) -> Unit,
) {

    SelectionBar(selectionManager) {

        var showLockConfirmationDialog by state { false }
        var showDeleteConfirmationDialog by state { false }

        Item(
            onClick = { showLockConfirmationDialog = true },
            text = "Lock",
        )

        Item(
            onClick = { showDeleteConfirmationDialog = true },
            text = "Delete",
        )

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
