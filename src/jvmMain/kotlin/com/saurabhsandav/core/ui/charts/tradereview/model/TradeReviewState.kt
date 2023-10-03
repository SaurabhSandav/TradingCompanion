package com.saurabhsandav.core.ui.charts.tradereview.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradeReviewState(
    val selectedProfileId: Long?,
    val tradesByDays: ImmutableList<TradesByDay>,
    val eventSink: (TradeReviewEvent) -> Unit,
) {

    @Immutable
    data class TradesByDay(
        val dayHeader: String,
        val trades: ImmutableList<TradeEntry>,
    )

    @Immutable
    internal data class TradeEntry(
        val isMarked: Boolean,
        val id: Long,
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
