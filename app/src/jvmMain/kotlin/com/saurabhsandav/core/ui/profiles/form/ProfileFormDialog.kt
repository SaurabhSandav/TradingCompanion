package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.theme.dimens

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

    val dialogState = rememberDialogState(size = DpSize(width = 250.dp, height = 300.dp))

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        title = state.title,
    ) {

        Box(Modifier.fillMaxSize()) {

            when (val model = state.formModel) {
                null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Form(
                    model = model,
                    trainingOnly = trainingOnly,
                )
            }
        }
    }
}

@Composable
private fun Form(
    model: ProfileFormModel,
    trainingOnly: Boolean,
) {

    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.dimens.containerPadding),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            value = model.nameField.value,
            onValueChange = { model.nameField.value = it },
            label = { Text("Name") },
            isError = model.nameField.isError,
            supportingText = model.nameField.errorMessage?.let { { Text(it) } },
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
