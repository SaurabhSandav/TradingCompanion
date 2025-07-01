package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.CoilZoomState
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormWindow
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.openExternally
import com.saurabhsandav.trading.record.model.AttachmentFileId
import java.io.File
import java.net.URI
import kotlin.math.pow

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
    var showImagePreview by state { false }

    ContextMenuArea(
        items = {
            buildList {

                if (attachment.type == TradeAttachment.Type.Image) {
                    add(ContextMenuItem("Open Externally") { File(attachment.path).openExternally() })
                }

                add(ContextMenuItem("Edit") { showEditDialog = true })
                add(ContextMenuItem("Remove") { showRemoveConfirmationDialog = true })
            }
        },
    ) {

        ListItem(
            modifier = Modifier.clickable {
                when (attachment.type) {
                    TradeAttachment.Type.Image -> showImagePreview = true
                    TradeAttachment.Type.Other -> File(attachment.path).openExternally()
                }
            },
            headlineContent = { Text(attachment.name) },
            overlineContent = attachment.extension?.let { { Text(it) } },
            supportingContent = attachment.description?.let { { Text(it) } },
            trailingContent = {

                Row {

                    IconButtonWithTooltip(
                        onClick = { showEditDialog = true },
                        tooltipText = "Edit Attachment",
                        content = {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Attachment")
                        },
                    )

                    IconButtonWithTooltip(
                        onClick = { showRemoveConfirmationDialog = true },
                        tooltipText = "Remove Attachment",
                        content = {
                            Icon(Icons.Default.Close, contentDescription = "Remove Attachment")
                        },
                    )
                }
            },
        )
    }

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

    if (showImagePreview) {

        AppDialog(
            onDismissRequest = { showImagePreview = false },
            surfaceColor = Color.Transparent,
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
            ) {

                val zoomState = rememberCoilZoomState()

                CoilZoomAsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.dimens.containerPadding)
                        .pointerInput(Unit) {
                            awaitEachGesture {

                                awaitFirstDown(pass = PointerEventPass.Initial)
                                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (upEvent != null) {
                                    val offset = upEvent.position

                                    if (offset !in zoomState.zoomable.contentDisplayRectF) {
                                        showImagePreview = false
                                    }
                                }
                            }
                        },
                    zoomState = zoomState,
                    model = attachment.path,
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Inside,
                )

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                ) {

                    ZoomControls(
                        zoomState = zoomState,
                    )

                    Button(
                        onClick = { File(attachment.path).openExternally() },
                    ) {
                        Text("Open Externally")
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomControls(zoomState: CoilZoomState) {

    Surface(
        shape = MaterialTheme.shapes.large,
    ) {

        Row {

            val scope = rememberCoroutineScope()
            val scaleX = zoomState.zoomable.transform.scaleX

            IconButtonWithTooltip(
                onClick = {

                    val targetScale = with(zoomState.zoomable) {
                        val tolerance = maxScale * 0.01F
                        val zoomFactor = (maxScale / minScale).pow(1F / ZoomSteps)
                        val targetScale = transform.scaleX / zoomFactor
                        if (targetScale - minScale < tolerance) minScale else targetScale
                    }

                    scope.launchUnit {
                        zoomState.zoomable.scale(targetScale, animated = true)
                    }
                },
                tooltipText = "Zoom Out",
                enabled = scaleX != zoomState.zoomable.minScale,
                content = {

                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                    )
                },
            )

            IconButtonWithTooltip(
                onClick = {
                    scope.launchUnit {
                        zoomState.zoomable.scale(1F, animated = true)
                    }
                },
                tooltipText = "Reset Zoom",
                enabled = scaleX != 1F,
                content = {

                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Reset Zoom",
                    )
                },
            )

            IconButtonWithTooltip(
                onClick = {

                    val targetScale = with(zoomState.zoomable) {
                        val tolerance = maxScale * 0.01F
                        val zoomFactor = (maxScale / minScale).pow(1F / ZoomSteps)
                        val targetScale = transform.scaleX * zoomFactor
                        if (maxScale - targetScale < tolerance) maxScale else targetScale
                    }

                    scope.launchUnit {
                        zoomState.zoomable.scale(targetScale, animated = true)
                    }
                },
                tooltipText = "Zoom In",
                enabled = scaleX != zoomState.zoomable.maxScale,
                content = {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                    )
                },
            )
        }
    }
}

private const val ZoomSteps = 5
