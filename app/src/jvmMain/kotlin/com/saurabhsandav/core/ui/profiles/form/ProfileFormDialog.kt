package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalAppGraph
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
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.profileFormGraphFactory
            .create(type)
            .presenterFactory
            .create(scope, onCloseRequest, trainingOnly)
    }
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
            onSubmit = state.onSubmit,
        )
    }
}

@Composable
private fun ProfileForm(
    model: ProfileFormModel,
    trainingOnly: Boolean,
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
            onValueChange = { model.nameField.holder.value = it },
            label = { Text("Name") },
            isError = model.nameField.isError,
            supportingText = model.nameField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.descriptionField.value,
            onValueChange = { model.descriptionField.holder.value = it },
            label = { Text("Description") },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Training", Modifier.weight(1F))

            Checkbox(
                checked = trainingOnly || model.isTrainingField.value,
                onCheckedChange = { model.isTrainingField.holder.value = it },
                enabled = !trainingOnly,
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = validator::submit,
            enabled = validator.canSubmit,
            content = { Text("Save Profile") },
        )
    }
}
