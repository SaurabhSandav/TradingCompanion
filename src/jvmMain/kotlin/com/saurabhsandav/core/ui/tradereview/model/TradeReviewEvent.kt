package com.saurabhsandav.core.ui.tradereview.model

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId

internal sealed class TradeReviewEvent {

    data class ProfileSelected(val id: ProfileId?) : TradeReviewEvent()

    data class MarkTrade(
        val profileTradeId: ProfileTradeId,
        val isMarked: Boolean,
    ) : TradeReviewEvent()

    data class SelectTrade(val profileTradeId: ProfileTradeId) : TradeReviewEvent()

    data class OpenDetails(val profileTradeId: ProfileTradeId) : TradeReviewEvent()

    data object ClearMarkedTrades : TradeReviewEvent()

    data class ApplyFilter(val tradeFilter: TradeFilter) : TradeReviewEvent()
}
