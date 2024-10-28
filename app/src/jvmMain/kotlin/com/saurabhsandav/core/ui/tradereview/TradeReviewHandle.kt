package com.saurabhsandav.core.ui.tradereview

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.SelectTrade
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class TradeReviewHandle {

    private val _events = Channel<TradeReviewEvent>(Channel.UNLIMITED)
    internal val events = _events.receiveAsFlow()

    fun markTrades(
        tradeIds: List<ProfileTradeId>,
        navigateToTrade: ProfileTradeId? = tradeIds.firstOrNull(),
    ) {

        if (navigateToTrade != null) _events.trySend(SelectTrade(navigateToTrade))

        tradeIds.forEach { profileTradeId ->
            _events.trySend(MarkTrade(profileTradeId, true))
        }
    }
}
