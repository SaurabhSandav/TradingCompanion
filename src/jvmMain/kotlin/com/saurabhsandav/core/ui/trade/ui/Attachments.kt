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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.saurabhsandav.core.trades.model.TradeAttachmentId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.form.rememberFormValidator
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trade.model.AttachmentFormModel
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeAttachment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File

@Composable
internal fun Attachments(
    attachments: ImmutableList<TradeAttachment>,
    onAddAttachment: (AttachmentFormModel) -> Unit,
    onUpdateAttachment: (TradeAttachmentId, AttachmentFormModel) -> Unit,
    onRemoveAttachment: (TradeAttachmentId) -> Unit,
) {

    Column(
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        // Header
        Box(
            modifier = Modifier.height(64.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center,
            content = { Text(text = "Attachments") },
        )

        Divider()

        attachments.take(10).forEach { attachment ->

            key(attachment) {

                AttachmentItem(
                    attachment = attachment,
                    onUpdate = { formModel -> onUpdateAttachment(attachment.id, formModel) },
                    onRemove = { onRemoveAttachment(attachment.id) },
                )
            }
        }

        Divider()

        var showAddAttachmentDialog by state { false }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showAddAttachmentDialog = true },
            shape = RectangleShape,
            content = { Text("Add Attachment") },
        )

        if (showAddAttachmentDialog) {

            AttachmentEditorDialog(
                initialModel = null,
                onSaveAttachment = onAddAttachment,
                onCloseRequest = { showAddAttachmentDialog = false },
            )
        }
    }
}

@Composable
internal fun AttachmentItem(
    attachment: TradeAttachment,
    onUpdate: (AttachmentFormModel) -> Unit,
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

        AttachmentEditorDialog(
            initialModel = remember {
                AttachmentFormModel.Initial(
                    name = attachment.name,
                )
            },
            onSaveAttachment = onUpdate,
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

@Composable
internal fun AttachmentEditorDialog(
    initialModel: AttachmentFormModel.Initial?,
    onSaveAttachment: (AttachmentFormModel) -> Unit,
    onCloseRequest: () -> Unit,
) {

    val isEditMode = initialModel != null

    var showFilePicker by state { !isEditMode }
    var showDetailsForm by state { isEditMode }
    var fileName by state<String?> { null }

    val formValidator = rememberFormValidator()
    val model = remember {
        AttachmentFormModel(
            validator = formValidator,
            initial = initialModel ?: AttachmentFormModel.Initial(),
        )
    }

    FilePicker(
        show = showFilePicker,
        title = "Select Attachment",
    ) { fileDetails ->

        when (fileDetails) {
            null -> onCloseRequest()
            else -> {

                showFilePicker = false
                showDetailsForm = true

                model.path = fileDetails.path
                fileName = (fileDetails.platformFile as File).name
            }
        }
    }

    if (showDetailsForm) {

        val dialogState = rememberDialogState(size = DpSize(width = 250.dp, height = 300.dp))

        AppDialogWindow(
            onCloseRequest = onCloseRequest,
            state = dialogState,
            title = when {
                initialModel != null -> "Edit Attachment"
                else -> "New Attachment"
            },
        ) {

            Column(
                modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                val initialFocusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

                OutlinedTextField(
                    modifier = Modifier.focusRequester(initialFocusRequester),
                    value = model.nameField.value,
                    onValueChange = { model.nameField.value = it.trim() },
                    label = { Text("Name") },
                    isError = model.nameField.isError,
                    supportingText = model.nameField.errorMessage?.let { { Text(it) } },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = model.descriptionField.value,
                    onValueChange = { model.descriptionField.value = it.trim() },
                    label = { Text("Description") },
                )

                if (!isEditMode)
                    Text("File to upload: $fileName")

                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        coroutineScope.launch {

                            if (formValidator.validate()) {
                                onSaveAttachment(model)
                                onCloseRequest()
                            }
                        }
                    },
                    enabled = model.validator.isValid,
                    content = { Text("Save Attachment") },
                )
            }
        }
    }
}
