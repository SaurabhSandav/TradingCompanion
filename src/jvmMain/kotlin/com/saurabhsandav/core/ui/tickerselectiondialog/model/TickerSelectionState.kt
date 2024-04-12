package com.saurabhsandav.core.ui.tickerselectiondialog.model

internal data class TickerSelectionState(
    val tickers: List<String>,
    val eventSink: (TickerSelectionEvent) -> Unit,
)
