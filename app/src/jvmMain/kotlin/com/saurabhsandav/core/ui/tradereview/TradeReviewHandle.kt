package com.saurabhsandav.core.ui.tradereview

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewEvent.SelectTrade
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class TradeReviewHandle {

    private val _events = Channel<TradeReviewEvent>(Channel.UNLIMITED)
    internal val events = _events.receiveAsFlow()

    fun markTrade(tradeId: ProfileTradeId) {
        _events.trySend(SelectTrade(tradeId))
    }
}
