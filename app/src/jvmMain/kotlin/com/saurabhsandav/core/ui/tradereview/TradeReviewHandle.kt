package com.saurabhsandav.core.ui.tradereview

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.SelectTrade
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class TradeReviewHandle {

    private val events = Channel<TradeReviewEvent>(Channel.UNLIMITED)
    internal val eventsFlow = events.receiveAsFlow()

    fun markTrades(
        tradeIds: List<ProfileTradeId>,
        navigateToTrade: ProfileTradeId? = tradeIds.firstOrNull(),
    ) {

        if (navigateToTrade != null) events.trySend(SelectTrade(navigateToTrade))

        tradeIds.forEach { profileTradeId ->
            events.trySend(MarkTrade(profileTradeId, true))
        }
    }
}
