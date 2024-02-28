package com.saurabhsandav.core.ui.review.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class ReviewState(
    val title: String,
    val isMarkdown: Boolean,
    val review: String,
    val trades: List<TradeEntry>,
    val eventSink: (ReviewEvent) -> Unit,
) {

    enum class Tab {
        Review,
        Trades,
    }

    @Immutable
    internal data class TradeEntry(
        val profileTradeId: ProfileTradeId,
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
