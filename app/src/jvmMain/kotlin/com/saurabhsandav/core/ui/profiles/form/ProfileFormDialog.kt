package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.FormDefaults
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError

@Composable
internal fun ProfileFormDialog(
    type: ProfileFormType,
    trainingOnly: Boolean,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember { screensModule.profileFormModule(scope).presenter(onCloseRequest, type, trainingOnly) }
    val state by presenter.state.collectAsState()

    val dialogState = rememberDialogState(size = DpSize(width = FormDefaults.PreferredWidth, height = 300.dp))

    val formModel = state.formModel ?: return

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        title = state.title,
    ) {

        ProfileForm(
            model = formModel,
            trainingOnly = trainingOnly,
        )
    }
}

@Composable
private fun ProfileForm(
    model: ProfileFormModel,
    trainingOnly: Boolean,
) {

    Form {

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            value = model.nameField.value,
            onValueChange = { model.nameField.value = it },
            label = { Text("Name") },
            isError = model.nameField.isError,
            supportingText = model.nameField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.descriptionField.value,
            onValueChange = { model.descriptionField.value = it },
            label = { Text("Description") },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Training", Modifier.weight(1F))

            Checkbox(
                checked = trainingOnly || model.isTrainingField.value,
                onCheckedChange = { model.isTrainingField.value = it },
                enabled = !trainingOnly,
            )
        }

        Button(
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text("Save Profile") },
        )
    }
}
