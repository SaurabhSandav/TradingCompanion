package com.saurabhsandav.core.ui.attachmentform

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.FormDefaults
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.onTextFieldClickOrEnter
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.utils.openExternally
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import io.github.vinceglb.filekit.core.pickFile
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

    val dialogState = rememberDialogState(size = DpSize(width = FormDefaults.PreferredWidth, height = 400.dp))

    val formModel = state.formModel ?: return

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        title = if (formType is Edit) "Edit Attachment" else "Add Attachment",
    ) {

        val fileKitPlatformSettings = remember { FileKitPlatformSettings(parentWindow = window) }

        AttachmentForm(
            formType = formType,
            model = formModel,
            fileKitPlatformSettings = fileKitPlatformSettings,
            onSubmit = state.onSubmit,
        )
    }
}

@Composable
private fun AttachmentForm(
    formType: AttachmentFormType,
    model: AttachmentFormModel,
    fileKitPlatformSettings: FileKitPlatformSettings,
    onSubmit: () -> Unit,
) {

    Form(
        formModels = listOf(model),
        onSubmit = onSubmit,
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
                modifier = Modifier.onTextFieldClickOrEnter { showFilePicker = true },
                value = model.path ?: "Select...",
                onValueChange = {},
                label = { Text("File") },
                readOnly = true,
            )
        }

        AnimatedVisibilityForNullable(model.path) { path ->

            TextButton(
                onClick = { File(path).openExternally() },
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
            modifier = Modifier.fillMaxWidth(),
            onClick = validator::submit,
            enabled = validator.canSubmit,
            content = { Text(if (formType is New) "Add" else "Save") },
        )
    }
}
