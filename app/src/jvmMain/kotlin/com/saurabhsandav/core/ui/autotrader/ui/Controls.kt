package com.saurabhsandav.core.ui.autotrader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.autotrader.model.ConfigFormModel
import com.saurabhsandav.core.ui.autotrader.model.ScriptFormModel
import com.saurabhsandav.core.ui.common.controls.DateRangePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.utils.NIFTY500
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun Controls(
    configFormModel: ConfigFormModel,
    scriptFormModel: ScriptFormModel?,
    isScriptRunning: Boolean,
    onSelectScript: () -> Unit,
    onRun: () -> Unit,
) {

    OutlinedListSelectionField(
        modifier = Modifier.fillMaxWidth(),
        items = remember { NIFTY500.toPersistentList().mutate { it.add(0, "All") } },
        itemText = { it },
        selection = configFormModel.tickerField.value,
        onSelection = { configFormModel.tickerField.value = it },
        label = { Text("Ticker") },
        isError = configFormModel.tickerField.isError,
        supportingText = configFormModel.tickerField.errorMessage?.let { { Text(it) } },
    )

    DateRangePickerField(
        modifier = Modifier.fillMaxWidth(),
        from = configFormModel.intervalField.value.start,
        to = configFormModel.intervalField.value.endInclusive,
        onValidValueChange = { from, to ->
            configFormModel.intervalField.value = from..to
        },
        label = { Text("Date Range") },
        format = "MMM dd, yyyy",
    )

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = configFormModel.titleField.value,
        onValueChange = { configFormModel.titleField.value = it },
        label = { Text("Title") },
        isError = configFormModel.titleField.isError,
        supportingText = configFormModel.titleField.errorMessage?.let { { Text(it) } },
    )

    Card(
        modifier = Modifier.clickable(
            enabled = scriptFormModel == null,
            onClick = onSelectScript,
        ),
    ) {

        Text(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            text = when {
                scriptFormModel != null -> scriptFormModel.titleField.value
                else -> "Select script"
            },
            textAlign = TextAlign.Center,
        )
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onRun,
        enabled = !isScriptRunning,
    ) {

        Text("Run")

        AnimatedVisibility(
            modifier = Modifier.padding(start = 16.dp).size(ButtonDefaults.IconSize),
            visible = isScriptRunning,
        ) {

            CircularProgressIndicator()
        }
    }
}
