package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.form2.FormValidator
import com.saurabhsandav.core.ui.common.form2.isError
import com.saurabhsandav.core.ui.common.form2.rememberFormValidator
import com.saurabhsandav.core.ui.profiles.model.ProfileFormModel
import kotlinx.coroutines.launch

@Composable
internal fun ProfileEditorDialog(
    formModel: ((FormValidator) -> ProfileFormModel)?,
    onSaveProfile: (ProfileFormModel) -> Unit,
    onCloseRequest: () -> Unit,
    trainingOnly: Boolean,
) {

    val dialogState = rememberDialogState(size = DpSize(width = 250.dp, height = 300.dp))

    val formValidator = rememberFormValidator()
    val model = remember {
        formModel?.invoke(formValidator) ?: ProfileFormModel(
            validator = formValidator,
            name = "",
            description = "",
            isTraining = true,
        )
    }

    AppDialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
        title = when {
            formModel != null -> "Edit Profile"
            else -> "New Profile"
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

            val coroutineScope = rememberCoroutineScope()

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (formValidator.validate()) {
                            onSaveProfile(model)
                            onCloseRequest()
                        }
                    }
                },
                enabled = model.validator.isValid,
                content = { Text("Save Profile") },
            )
        }
    }
}
