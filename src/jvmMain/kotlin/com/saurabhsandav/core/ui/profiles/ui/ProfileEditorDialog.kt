package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.profiles.model.ProfileFormModel

@Composable
internal fun ProfileEditorDialog(
    formModel: ((FormValidator) -> ProfileFormModel)?,
    onSaveProfile: (ProfileFormModel) -> Unit,
    onCloseRequest: () -> Unit,
    trainingOnly: Boolean,
) {

    val dialogState = rememberDialogState(size = DpSize(width = 250.dp, height = 300.dp))

    val formValidator = remember { FormValidator() }
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

            OutlinedTextField(
                value = model.name.value,
                onValueChange = { model.name.value = it },
                label = { Text("Name") },
                isError = model.name.isError,
                supportingText = model.name.errorMessage?.let { { Text(it) } },
                singleLine = true,
            )

            OutlinedTextField(
                value = model.description.value,
                onValueChange = { model.description.value = it },
                label = { Text("Description") },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Training", Modifier.weight(1F))

                Checkbox(
                    checked = trainingOnly || model.isTraining.value,
                    onCheckedChange = { model.isTraining.value = it },
                    enabled = !trainingOnly,
                )
            }

            Button(
                onClick = {
                    if (formValidator.isValid()) {
                        onSaveProfile(model)
                        onCloseRequest()
                    }
                },
            ) {

                Text("Save Profile")
            }
        }
    }
}
