package com.saurabhsandav.core.ui.charts.tradereview.model

internal sealed class TradeReviewEvent {

    data class SelectProfile(val id: Long) : TradeReviewEvent()

    data class MarkTrade(
        val id: Long,
        val isMarked: Boolean,
    ) : TradeReviewEvent()

    data class SelectTrade(val id: Long) : TradeReviewEvent()
}
