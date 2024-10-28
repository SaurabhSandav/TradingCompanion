package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.SelectionBar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun TradesSelectionBar(
    profileId: ProfileId,
    selectionManager: SelectionManager<TradeId>,
    onDeleteTrades: (List<TradeId>) -> Unit,
) {

    SelectionBar(selectionManager) {

        var showDeleteConfirmationDialog by state { false }

        Item(
            onClick = { showDeleteConfirmationDialog = true },
            text = "Delete",
        )

        var showAddAttachmentDialog by state { false }

        Item(
            onClick = { showAddAttachmentDialog = true },
            text = "Add Attachment",
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

        if (showAddAttachmentDialog) {

            AttachmentFormWindow(
                profileId = profileId,
                formType = remember { AttachmentFormType.New(selectionManager.selection.toList()) },
                onCloseRequest = {
                    showAddAttachmentDialog = false
                    selectionManager.clear()
                },
            )
        }
    }
}
