package com.saurabhsandav.core.ui.tradereview.model

import app.cash.paging.PagingData
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow

internal data class TradeReviewState(
    val selectedProfileId: ProfileId?,
    val selectedProfileName: String?,
    val trades: Flow<PagingData<TradeItem>>,
    val markedTrades: List<MarkedTradeItem>,
    val eventSink: (TradeReviewEvent) -> Unit,
) {

    enum class Tab {
        Profile,
        Marked,
    }

    internal data class TradeItem(
        val profileTradeId: ProfileTradeId,
        val isMarked: Boolean,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val entryTime: String,
        val duration: Flow<String>,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )

    internal data class MarkedTradeItem(
        val profileTradeId: ProfileTradeId,
        val profileName: String,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val entryTime: String,
        val duration: Flow<String>,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )
}
