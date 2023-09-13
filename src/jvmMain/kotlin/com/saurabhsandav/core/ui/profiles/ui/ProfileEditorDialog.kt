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
import com.saurabhsandav.core.ui.profiles.model.ProfileModel

@Composable
internal fun ProfileEditorDialog(
    profileModel: ((FormValidator) -> ProfileModel)?,
    onSaveProfile: (ProfileModel) -> Unit,
    onCloseRequest: () -> Unit,
    trainingOnly: Boolean,
) {

    val dialogState = rememberDialogState(size = DpSize(width = 250.dp, height = 300.dp))

    val formValidator = remember { FormValidator() }
    val formModel = remember {
        profileModel?.invoke(formValidator) ?: ProfileModel(
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
            profileModel != null -> "Edit Profile"
            else -> "New Profile"
        },
    ) {

        Column(
            modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            OutlinedTextField(
                value = formModel.name.value,
                onValueChange = { formModel.name.value = it },
                label = { Text("Name") },
                isError = formModel.name.isError,
                supportingText = formModel.name.errorMessage?.let { { Text(it) } },
                singleLine = true,
            )

            OutlinedTextField(
                value = formModel.description.value,
                onValueChange = { formModel.description.value = it },
                label = { Text("Description") },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Training", Modifier.weight(1F))

                Checkbox(
                    checked = trainingOnly || formModel.isTraining.value,
                    onCheckedChange = { formModel.isTraining.value = it },
                    enabled = !trainingOnly,
                )
            }

            Button(
                onClick = {
                    if (formValidator.isValid()) {
                        onSaveProfile(formModel)
                        onCloseRequest()
                    }
                },
            ) {

                Text("Save Profile")
            }
        }
    }
}
