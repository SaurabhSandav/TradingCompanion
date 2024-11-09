package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import java.awt.Desktop
import java.io.File
import java.net.URI

@Composable
internal fun Attachments(
    profileTradeId: ProfileTradeId,
    attachments: List<TradeAttachment>,
    onRemoveAttachment: (AttachmentFileId) -> Unit,
    modifier: Modifier = Modifier,
) {

    var showAddAttachmentDialog by state { false }
    var path by state<String?> { null }

    val callback = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {

                val file = (event.dragData() as DragData.FilesList).readFiles().firstOrNull() ?: return false

                showAddAttachmentDialog = true
                path = URI.create(file).path

                return true
            }
        }
    }

    TradeSection(
        modifier = Modifier.then(modifier).dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.action == DragAndDropTransferAction.Copy && event.dragData() is DragData.FilesList
            },
            target = callback,
        ),
        title = "Attachments",
        subtitle = when {
            attachments.isEmpty() -> "No Attachments"
            attachments.size == 1 -> "1 Attachment"
            else -> "${attachments.size} Attachments"
        },
        trailingContent = {

            TradeSectionButton(
                onClick = { showAddAttachmentDialog = true },
                text = "Add Attachment",
            )
        },
    ) {

        attachments.forEach { attachment ->

            key(attachment) {

                AttachmentItem(
                    profileTradeId = profileTradeId,
                    attachment = attachment,
                    onRemove = { onRemoveAttachment(attachment.fileId) },
                )

                HorizontalDivider()
            }
        }

        if (showAddAttachmentDialog) {

            AttachmentFormWindow(
                profileId = profileTradeId.profileId,
                formType = remember { AttachmentFormType.New(listOf(profileTradeId.tradeId), path) },
                onCloseRequest = {
                    showAddAttachmentDialog = false
                    path = null
                },
            )
        }
    }
}

@Composable
internal fun AttachmentItem(
    profileTradeId: ProfileTradeId,
    attachment: TradeAttachment,
    onRemove: () -> Unit,
) {

    var showEditDialog by state { false }
    var showRemoveConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable {

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                error("Opening attachment not supported")
            }

            Desktop.getDesktop().open(File(attachment.path))
        },
        headlineContent = { Text(attachment.name) },
        overlineContent = attachment.extension?.let { { Text(it) } },
        supportingContent = attachment.description?.let { { Text(it) } },
        trailingContent = {

            Row {

                IconButtonWithTooltip(
                    onClick = { showEditDialog = true },
                    tooltipText = "Edit attachment",
                    content = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit attachment")
                    },
                )

                IconButtonWithTooltip(
                    onClick = { showRemoveConfirmationDialog = true },
                    tooltipText = "Remove attachment",
                    content = {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    },
                )
            }
        },
    )

    if (showEditDialog) {

        AttachmentFormWindow(
            profileId = profileTradeId.profileId,
            formType = remember { AttachmentFormType.Edit(profileTradeId.tradeId, attachment.fileId) },
            onCloseRequest = { showEditDialog = false },
        )
    }

    if (showRemoveConfirmationDialog) {

        ConfirmationDialog(
            text = "Are you sure you want to remove the attachment?",
            onDismiss = { showRemoveConfirmationDialog = false },
            onConfirm = onRemove,
        )
    }
}
