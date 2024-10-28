package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.SelectionBar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun TradesSelectionBar(
    selectionManager: SelectionManager<TradeId>,
    onDeleteTrades: (List<TradeId>) -> Unit,
) {

    SelectionBar(selectionManager) {

        var showDeleteConfirmationDialog by state { false }

        Item(
            onClick = { showDeleteConfirmationDialog = true },
            text = "Delete",
        )

        if (showDeleteConfirmationDialog) {

            DeleteConfirmationDialog(
                subject = "trades",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = {
                    onDeleteTrades(selectionManager.selection.toList())
                    selectionManager.clear()
                },
            )
        }
    }
}
