package com.saurabhsandav.core.ui.tickerselectiondialog.model

internal sealed class TickerSelectionEvent {

    data class Filter(val query: String) : TickerSelectionEvent()
}
