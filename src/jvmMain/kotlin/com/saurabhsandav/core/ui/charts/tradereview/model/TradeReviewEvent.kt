package com.saurabhsandav.core.ui.charts.tradereview.model

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

internal sealed class TradeReviewEvent {

    data class SelectProfile(val id: ProfileId) : TradeReviewEvent()

    data class MarkTrade(
        val profileTradeId: ProfileTradeId,
        val isMarked: Boolean,
    ) : TradeReviewEvent()

    data class SelectTrade(val profileTradeId: ProfileTradeId) : TradeReviewEvent()

    data class OpenDetails(val profileTradeId: ProfileTradeId) : TradeReviewEvent()

    data object ClearMarkedTrades : TradeReviewEvent()
}
