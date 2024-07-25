package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                // Must be PointerEventPass.Initial to observe events before the text field consumes them
                // in the Main pass
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) {
                    showTickerSelectionDialog = !showTickerSelectionDialog
                }
            }
        },
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
