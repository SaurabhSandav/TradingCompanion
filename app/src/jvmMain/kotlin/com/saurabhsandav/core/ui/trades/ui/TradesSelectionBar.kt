package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.SelectionBar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.selector.TagSelectorDropdownMenu
import com.saurabhsandav.core.ui.tags.selector.TagSelectorType

@Composable
internal fun TradesSelectionBar(
    profileId: ProfileId,
    selectionManager: SelectionManager<TradeId>,
    onDeleteTrades: (List<TradeId>) -> Unit,
    onAddTag: (List<TradeId>, TradeTagId) -> Unit,
    onOpenChart: (List<TradeId>) -> Unit,
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

        var expanded by state { false }

        AddTagContainer(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            profileId =  profileId,
            tagSelectorType = { TagSelectorType.ForTrades(selectionManager.selection.toList()) },
            onAddTag = { tagId ->
                onAddTag(selectionManager.selection.toList(), tagId)
                selectionManager.clear()
            },
        ) {

            Item(
                onClick = { expanded = true },
                text = "Add Tag",
            )
        }

        Item(
            onClick = {
                onOpenChart(selectionManager.selection.toList())
                selectionManager.clear()
            },
            text = "Chart",
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

@Composable
private fun AddTagContainer(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    profileId: ProfileId,
    tagSelectorType: () -> TagSelectorType,
    onAddTag: (TradeTagId) -> Unit,
    item: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopCenter),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {

        item()

        TagSelectorDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            profileId = profileId,
            type = tagSelectorType,
            onSelectTag = onAddTag,
        )
    }
}
