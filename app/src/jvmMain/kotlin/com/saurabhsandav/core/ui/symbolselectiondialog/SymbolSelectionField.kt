package com.saurabhsandav.core.ui.symbolselectiondialog

import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.onTextFieldClickOrEnter
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.trading.core.SymbolId

@Composable
fun SymbolSelectionField(
    type: SymbolSelectionType,
    selected: SymbolId?,
    onSelect: (SymbolId) -> Unit,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var showSymbolSelectionDialog by state { false }

    OutlinedTextField(
        modifier = Modifier.onTextFieldClickOrEnter { showSymbolSelectionDialog = true },
        value = selected?.value ?: "",
        onValueChange = {},
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        label = { Text("Symbol") },
        placeholder = { Text("Select...") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSymbolSelectionDialog) },
        supportingText = supportingText,
        isError = isError,
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    if (showSymbolSelectionDialog) {

        SymbolSelectionDialog(
            onDismissRequest = { showSymbolSelectionDialog = false },
            onSelect = onSelect,
            type = type,
        )
    }
}
