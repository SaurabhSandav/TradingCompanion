package com.saurabhsandav.core.ui.symbolselectiondialog

import com.saurabhsandav.trading.core.SymbolId

sealed class SymbolSelectionType {

    data object Regular : SymbolSelectionType()

    data class Chart(
        val onOpenInCurrentWindow: ((SymbolId) -> Unit)?,
        val onOpenInNewWindow: (SymbolId) -> Unit,
    ) : SymbolSelectionType()
}
