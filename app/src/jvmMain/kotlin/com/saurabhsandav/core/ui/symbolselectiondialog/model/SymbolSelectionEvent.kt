package com.saurabhsandav.core.ui.symbolselectiondialog.model

import com.saurabhsandav.trading.core.SymbolId

internal sealed class SymbolSelectionEvent {

    data class SymbolSelected(
        val id: SymbolId,
    ) : SymbolSelectionEvent()
}
