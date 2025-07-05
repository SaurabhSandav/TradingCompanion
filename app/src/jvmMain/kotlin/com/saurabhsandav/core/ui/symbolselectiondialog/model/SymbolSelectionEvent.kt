package com.saurabhsandav.core.ui.symbolselectiondialog.model

import androidx.compose.ui.text.input.TextFieldValue
import com.saurabhsandav.trading.core.SymbolId

internal sealed class SymbolSelectionEvent {

    data class SymbolSelected(
        val id: SymbolId,
    ) : SymbolSelectionEvent()

    data class Filter(
        val query: TextFieldValue,
    ) : SymbolSelectionEvent()
}
