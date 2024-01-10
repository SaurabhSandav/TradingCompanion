package com.saurabhsandav.core.ui.tagform

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
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.tagform.model.TagFormModel
import com.saurabhsandav.core.ui.tagform.model.TagFormType

@Composable
fun TagFormWindow(
    profileId: ProfileId,
    formType: TagFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.tagFormModule(scope).presenter(profileId, formType, onCloseRequest) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(size = DpSize(width = 250.dp, height = 300.dp))

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
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
