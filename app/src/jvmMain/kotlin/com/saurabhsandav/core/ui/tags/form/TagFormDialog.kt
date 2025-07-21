package com.saurabhsandav.core.ui.tags.form

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.ColorPickerDialog
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.model.TagFormModel
import com.saurabhsandav.core.ui.tags.form.model.TagFormType
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun TagFormDialog(
    profileId: ProfileId,
    formType: TagFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.tagFormGraphFactory
            .create(profileId, formType)
            .presenterFactory
            .create(onCloseRequest, scope)
    }
    val state by presenter.state.collectAsState()

    val formModel = state.formModel ?: return

    AppDialog(
        onDismissRequest = onCloseRequest,
    ) {

        TagForm(
            formType = formType,
            model = formModel,
            onDelete = presenter::onDelete,
            onCloseRequest = onCloseRequest,
            onSubmit = state.onSubmit,
        )
    }
}

@Composable
private fun TagForm(
    formType: TagFormType,
    model: TagFormModel,
    onDelete: () -> Unit,
    onCloseRequest: () -> Unit,
    onSubmit: () -> Unit,
) {

    Form(
        formModels = listOf(model),
        onSubmit = onSubmit,
        width = 600.dp,
        scrollState = null,
    ) {

        val initialFocusRequester = remember { FocusRequester() }
        var showColorPicker by state { false }
        var showDeleteConfirmationDialog by state { false }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        Text(
            text = when (formType) {
                is TagFormType.New, is TagFormType.NewFromExisting -> "New Tag"
                is TagFormType.Edit -> "Edit Tag"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().focusRequester(initialFocusRequester),
            value = model.nameField.value,
            onValueChange = { model.nameField.value = it },
            label = { Text("Name") },
            isError = model.nameField.isError,
            supportingText = model.nameField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().weight(1F, fill = false),
            value = model.descriptionField.value,
            onValueChange = { model.descriptionField.value = it },
            label = { Text("Description") },
            minLines = 3,
        )

        AnimatedContent(
            targetState = model.colorField.value,
            contentKey = { it == null },
        ) { targetColor ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                if (targetColor != null) {

                    val animatedColor by animateColorAsState(targetColor)

                    Box(
                        modifier = Modifier
                            .weight(1F)
                            .height(ButtonDefaults.MinHeight)
                            .drawBehind { drawRect(animatedColor) }
                            .clickable { showColorPicker = true },
                    )

                    IconButton(onClick = { model.colorField.value = null }) {

                        Icon(Icons.Default.Close, contentDescription = "Remove color")
                    }
                } else {

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showColorPicker = true },
                        content = { Text("Select color") },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
        ) {

            if (formType is TagFormType.Edit) {

                Button(
                    modifier = Modifier.weight(1F),
                    onClick = { showDeleteConfirmationDialog = true },
                    content = { Text("Delete") },
                )
            }

            Button(
                modifier = Modifier.weight(1F),
                onClick = this@Form.validator::submit,
                enabled = this@Form.validator.canSubmit,
                content = { Text("Save") },
            )
        }

        if (showColorPicker) {

            ColorPickerDialog(
                onDismissRequest = { showColorPicker = false },
                onColorSelected = { model.colorField.value = it },
                initialSelection = model.colorField.value,
            )
        }

        if (showDeleteConfirmationDialog) {

            DeleteConfirmationDialog(
                subject = "tag",
                onDismiss = { showDeleteConfirmationDialog = false },
                onConfirm = {
                    onDelete()
                    onCloseRequest()
                },
            )
        }
    }
}
