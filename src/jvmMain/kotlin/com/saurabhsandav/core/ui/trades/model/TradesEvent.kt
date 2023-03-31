package com.saurabhsandav.core.ui.trades.model

import com.saurabhsandav.core.ui.trades.model.TradesState.ProfileTradeId

internal sealed class TradesEvent {

    data class OpenDetails(val profileTradeId: ProfileTradeId) : TradesEvent()

    data class CloseDetails(val profileTradeId: ProfileTradeId) : TradesEvent()

    object DetailsBroughtToFront : TradesEvent()

    data class OpenChart(val profileTradeId: ProfileTradeId) : TradesEvent()
}
