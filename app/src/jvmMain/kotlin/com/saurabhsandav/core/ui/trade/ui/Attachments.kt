package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import java.awt.Desktop
import java.io.File

@Composable
internal fun Attachments(
    profileTradeId: ProfileTradeId,
    attachments: List<TradeAttachment>,
    onRemoveAttachment: (AttachmentFileId) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        // Header
        Box(
            modifier = Modifier.height(MaterialTheme.dimens.listHeaderHeight).fillMaxWidth(),
            contentAlignment = Alignment.Center,
            content = { Text(text = "Attachments") },
        )

        HorizontalDivider()

        attachments.take(10).forEach { attachment ->

            key(attachment) {

                AttachmentItem(
                    profileTradeId = profileTradeId,
                    attachment = attachment,
                    onRemove = { onRemoveAttachment(attachment.fileId) },
                )
            }
        }

        HorizontalDivider()

        var showAddAttachmentDialog by state { false }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showAddAttachmentDialog = true },
            shape = RectangleShape,
            content = { Text("Add Attachment") },
        )

        if (showAddAttachmentDialog) {

            AttachmentFormWindow(
                profileId = profileTradeId.profileId,
                formType = remember { AttachmentFormType.New(listOf(profileTradeId.tradeId)) },
                onCloseRequest = { showAddAttachmentDialog = false },
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
