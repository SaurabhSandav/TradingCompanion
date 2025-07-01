package com.saurabhsandav.core.ui.tradereview.model

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.trading.record.model.TradeFilter

internal sealed class TradeReviewEvent {

    data class ProfileSelected(
        val id: ProfileId?,
    ) : TradeReviewEvent()

    data class MarkTrade(
        val profileTradeId: ProfileTradeId,
        val isMarked: Boolean,
    ) : TradeReviewEvent()

    data class SelectTrade(
        val profileTradeId: ProfileTradeId,
    ) : TradeReviewEvent()

    data class OpenDetails(
        val profileTradeId: ProfileTradeId,
    ) : TradeReviewEvent()

    data object MarkAllTrades : TradeReviewEvent()

    data object ClearMarkedTrades : TradeReviewEvent()

    data class ApplyFilter(
        val tradeFilter: TradeFilter,
    ) : TradeReviewEvent()
}
