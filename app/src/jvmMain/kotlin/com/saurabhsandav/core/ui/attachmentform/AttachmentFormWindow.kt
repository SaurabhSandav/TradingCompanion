package com.saurabhsandav.core.ui.attachmentform

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormModel
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType.Edit
import com.saurabhsandav.core.ui.attachmentform.model.AttachmentFormType.New
import com.saurabhsandav.core.ui.common.AnimatedVisibilityForNullable
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import io.github.vinceglb.filekit.core.pickFile
import java.awt.Desktop
import java.io.File

@Composable
internal fun AttachmentFormWindow(
    profileId: ProfileId,
    formType: AttachmentFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember {
        screensModule.attachmentFormModule(scope).presenter(onCloseRequest, profileId, formType)
    }
    val state by presenter.state.collectAsState()

    val dialogState = rememberDialogState(size = DpSize(width = 300.dp, height = 400.dp))

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        title = if (formType is Edit) "Edit Attachment" else "Add Attachment",
    ) {

        val fileKitPlatformSettings = remember { FileKitPlatformSettings(parentWindow = window) }

        AttachmentFormScreen(
            formType = formType,
            formModel = state.formModel,
            fileKitPlatformSettings = fileKitPlatformSettings,
        )
    }
}

@Composable
private fun AttachmentFormScreen(
    formType: AttachmentFormType,
    formModel: AttachmentFormModel?,
    fileKitPlatformSettings: FileKitPlatformSettings,
) {

    when {
        formModel != null -> AttachmentForm(
            formType = formType,
            model = formModel,
            fileKitPlatformSettings = fileKitPlatformSettings,
        )

        else -> CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
    }
}

@Composable
private fun AttachmentForm(
    formType: AttachmentFormType,
    model: AttachmentFormModel,
    fileKitPlatformSettings: FileKitPlatformSettings,
) {

    Column(
        modifier = Modifier.padding(MaterialTheme.dimens.containerPadding).width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.columnVerticalSpacing,
            alignment = Alignment.CenterVertically,
        ),
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
            supportingText = model.nameField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.descriptionField.value,
            onValueChange = { model.descriptionField.value = it.trim() },
            label = { Text("Description") },
        )

        if (formType is New) {

            var showFilePicker by state { formType.showPickerOnOpen }

            LaunchedEffect(showFilePicker) {

                if (!showFilePicker) return@LaunchedEffect

                model.path = FileKit.pickFile(
                    title = "Select Attachment",
                    platformSettings = fileKitPlatformSettings,
                )?.path ?: model.path

                showFilePicker = false
            }

            OutlinedTextField(
                modifier = Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: return@awaitEachGesture

                        showFilePicker = true
                    }
                },
                value = model.path ?: "Select...",
                onValueChange = { },
                label = { Text("File") },
                readOnly = true,
            )
        }

        AnimatedVisibilityForNullable(model.path) { path ->

            TextButton(
                onClick = {

                    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        error("Opening attachment not supported")
                    }

                    Desktop.getDesktop().open(File(path))
                },
            ) {

                Text("Open File")

                Spacer(Modifier.width(ButtonDefaults.IconSpacing))

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open file",
                )
            }
        }

        Button(
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text(if (formType is New) "Add" else "Save") },
        )
    }
}
