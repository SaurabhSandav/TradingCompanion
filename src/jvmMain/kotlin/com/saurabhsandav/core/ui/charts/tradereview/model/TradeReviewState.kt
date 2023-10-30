package com.saurabhsandav.core.ui.charts.tradereview.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradeReviewState(
    val selectedProfileId: ProfileId?,
    val trades: ImmutableList<TradeEntry>,
    val eventSink: (TradeReviewEvent) -> Unit,
) {

    @Immutable
    internal data class TradeEntry(
        val isMarked: Boolean,
        val id: TradeId,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val duration: Flow<String>,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )
}
