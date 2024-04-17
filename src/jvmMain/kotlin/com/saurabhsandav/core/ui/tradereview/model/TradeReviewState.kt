package com.saurabhsandav.core.ui.tradereview.model

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow

internal data class TradeReviewState(
    val selectedProfileId: ProfileId?,
    val selectedProfileName: String?,
    val trades: List<TradeEntry>,
    val markedTrades: List<MarkedTradeEntry>,
    val eventSink: (TradeReviewEvent) -> Unit,
) {

    enum class Tab {
        Profile,
        Marked,
    }

    internal data class TradeEntry(
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

    internal data class MarkedTradeEntry(
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
