package com.saurabhsandav.core.ui.symbolselectiondialog.model

import androidx.compose.ui.text.input.TextFieldValue

internal sealed class SymbolSelectionEvent {

    data class Filter(
        val query: TextFieldValue,
    ) : SymbolSelectionEvent()
}
