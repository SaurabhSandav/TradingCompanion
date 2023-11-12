package com.saurabhsandav.core.ui.tags.form

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.form.isError

@Composable
internal fun TagFormDialog(
    profileId: ProfileId,
    type: TagFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tagFormModule(scope).presenter(profileId, type, onCloseRequest) }
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
                    onSaveTag = presenter::save,
                )
            }
        }
    }
}

@Composable
private fun Form(
    model: TagFormModel,
    onSaveTag: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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

        Button(
            onClick = onSaveTag,
            enabled = model.validator.isValid,
            content = { Text("Save Tag") },
        )
    }
}
