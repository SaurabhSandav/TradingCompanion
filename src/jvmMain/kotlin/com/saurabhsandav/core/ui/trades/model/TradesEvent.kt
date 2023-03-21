package com.saurabhsandav.core.ui.trades.model

internal sealed class TradesEvent {

    data class OpenDetails(val id: Long) : TradesEvent()

    data class CloseDetails(val id: Long) : TradesEvent()

    object DetailsBroughtToFront : TradesEvent()

    data class OpenChart(val id: Long) : TradesEvent()
}
