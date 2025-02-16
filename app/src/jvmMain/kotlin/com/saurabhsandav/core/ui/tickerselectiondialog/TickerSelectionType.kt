package com.saurabhsandav.core.ui.tickerselectiondialog

sealed class TickerSelectionType {

    data object Regular : TickerSelectionType()

    data class Chart(
        val onOpenInCurrentWindow: ((String) -> Unit)?,
        val onOpenInNewWindow: (String) -> Unit,
    ) : TickerSelectionType()
}
