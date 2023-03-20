package com.saurabhsandav.core.ui.charts.tradereview.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradeReviewState(
    val tradesItems: ImmutableList<TradeListItem>,
) {

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
        val duration: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )

    @Immutable
    internal sealed class TradeListItem {

        @Immutable
        internal data class DayHeader(val header: String) : TradeListItem()

        @Immutable
        internal data class Entries(val entries: ImmutableList<TradeEntry>) : TradeListItem()
    }
}
