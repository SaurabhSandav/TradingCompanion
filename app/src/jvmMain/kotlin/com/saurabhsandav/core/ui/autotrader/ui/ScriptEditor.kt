package com.saurabhsandav.core.ui.autotrader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.autotrader.model.ScriptFormModel
import com.saurabhsandav.core.ui.common.form.isError

@Composable
internal fun ScriptEditor(
    formModel: ScriptFormModel,
    onFormatScript: () -> Unit,
    onSaveScript: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        TextField(
            modifier = Modifier.fillMaxWidth().focusRequester(initialFocusRequester),
            value = formModel.titleField.value,
            onValueChange = { formModel.titleField.value = it },
            label = { Text("Title") },
            isError = formModel.titleField.isError,
            supportingText = formModel.titleField.errorMessage?.let { { Text(it) } },
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = formModel.descriptionField.value,
            onValueChange = { formModel.descriptionField.value = it },
            label = { Text("Description") },
        )

        TextField(
            modifier = Modifier.fillMaxWidth().weight(.8F),
            value = formModel.scriptField.value,
            onValueChange = { formModel.scriptField.value = it },
            isError = formModel.scriptField.isError,
        )

        Text(
            modifier = Modifier.fillMaxWidth().weight(.2F),
            text = formModel.consoleText,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Button(
                modifier = Modifier.weight(1F),
                onClick = onFormatScript,
                content = { Text("Format") },
            )

            Button(
                modifier = Modifier.weight(1F),
                onClick = onSaveScript,
                enabled = formModel.canSave,
                content = { Text("Save") },
            )
        }
    }
}
