package com.saurabhsandav.core.ui.charts.tradereview.model

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId

internal sealed class TradeReviewEvent {

    data class SelectProfile(val id: ProfileId) : TradeReviewEvent()

    data class MarkTrade(
        val id: TradeId,
        val isMarked: Boolean,
    ) : TradeReviewEvent()

    data class SelectTrade(val id: TradeId) : TradeReviewEvent()

    data class OpenDetails(val id: TradeId) : TradeReviewEvent()
}
