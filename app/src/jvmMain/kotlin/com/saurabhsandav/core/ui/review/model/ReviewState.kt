package com.saurabhsandav.core.ui.review.model

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.flow.Flow

internal data class ReviewState(
    val title: String,
    val review: String,
    val trades: List<TradeEntry>,
    val eventSink: (ReviewEvent) -> Unit,
) {

    enum class Tab {
        Review,
        Trades,
    }

    internal data class TradeEntry(
        val profileTradeId: ProfileTradeId,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val entryTime: String,
        val duration: Duration,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    ) {

        sealed class Duration {

            data class Open(
                val flow: Flow<String>,
            ) : Duration()

            data class Closed(
                val str: String,
            ) : Duration()
        }
    }
}
