package com.saurabhsandav.core.ui.charts.tradereview.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradeReviewState(
    val selectedProfileId: ProfileId?,
    val trades: ImmutableList<TradeEntry>,
    val markedTrades: ImmutableList<MarkedTradeEntry>,
    val eventSink: (TradeReviewEvent) -> Unit,
) {

    enum class Tab {
        Profile,
        Marked,
    }

    @Immutable
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

    @Immutable
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
