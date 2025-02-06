package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.onTextFieldClickOrEnter
import com.saurabhsandav.core.ui.common.state

@Composable
fun TickerSelectionField(
    type: TickerSelectionType,
    tickers: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {

    var showTickerSelectionDialog by state { false }

    OutlinedTextField(
        modifier = Modifier.onTextFieldClickOrEnter { showTickerSelectionDialog = true },
        value = selected ?: "",
        onValueChange = {},
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        label = { Text("Ticker") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTickerSelectionDialog) },
        supportingText = supportingText,
        isError = isError,
        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    )

    if (showTickerSelectionDialog) {

        TickerSelectionDialog(
            onCloseRequest = { showTickerSelectionDialog = false },
            tickers = tickers,
            onSelect = onSelect,
            type = type,
        )
    }
}
