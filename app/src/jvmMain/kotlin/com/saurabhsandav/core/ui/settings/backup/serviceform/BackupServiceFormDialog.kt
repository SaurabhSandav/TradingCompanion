package com.saurabhsandav.core.ui.settings.backup.serviceform

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.settings.backup.BackupSettingsGraph
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.Edit
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.New

@Composable
internal fun BackupServiceFormDialog(
    backupSettingsGraph: BackupSettingsGraph,
    onDismissRequest: () -> Unit,
    formType: BackupServiceFormType,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember {
        backupSettingsGraph.backupServiceFormGraphFactory
            .create(formType)
            .presenterFactory
            .create(onDismissRequest, scope)
    }

    val serviceBuilder = presenter.serviceBuilder ?: return
    val formModel = serviceBuilder.formModel ?: return

    AppDialog(
        onDismissRequest = onDismissRequest,
    ) {

        Form(
            formType = formType,
            formModel = formModel,
            onSubmit = presenter::onSubmit,
            serviceForm = {

                with(serviceBuilder) {
                    Form()
                }
            },
        )
    }
}

@Composable
private fun Form(
    formType: BackupServiceFormType,
    formModel: BackupServiceFormModel,
    onSubmit: () -> Unit,
    serviceForm: @Composable ColumnScope.() -> Unit,
) {

    Form(
        formModels = listOf(formModel),
        onSubmit = onSubmit,
        scrollState = null,
    ) {

        Text(
            text = when (formType) {
                is New -> "New Service"
                is Edit -> "Edit Service"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = formModel.nameField.value,
            onValueChange = { value -> formModel.nameField.holder.value = value },
            isError = formModel.nameField.isError,
            label = { Text("Name") },
            supportingText = formModel.nameField.errorsMessagesAsSupportingText(),
        )

        serviceForm()

        HorizontalDivider()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = validator::submit,
            enabled = validator.canSubmit,
        ) {
            Text("Save")
        }
    }
}
