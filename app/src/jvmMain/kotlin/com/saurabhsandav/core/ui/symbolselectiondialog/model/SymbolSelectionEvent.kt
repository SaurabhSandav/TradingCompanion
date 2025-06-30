package com.saurabhsandav.core.ui.symbolselectiondialog.model

internal sealed class SymbolSelectionEvent {

    data class Filter(
        val query: String,
    ) : SymbolSelectionEvent()
}
