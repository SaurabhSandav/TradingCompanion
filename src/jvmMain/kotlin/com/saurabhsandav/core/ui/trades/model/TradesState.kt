package com.saurabhsandav.core.ui.trades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradesState(
    val tradesList: TradesList,
    val errors: ImmutableList<UIErrorMessage>,
    val eventSink: (TradesEvent) -> Unit,
) {

    @Immutable
    sealed class TradesList {

        @Immutable
        data class All(
            val trades: ImmutableList<TradeEntry>,
        ) : TradesList()

        @Immutable
        data class Focused(
            val openTrades: ImmutableList<TradeEntry>,
            val todayTrades: ImmutableList<TradeEntry>,
            val todayStats: Stats?,
            val pastTrades: ImmutableList<TradeEntry>,
        ) : TradesList()
    }

    @Immutable
    internal data class Stats(
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )

    @Immutable
    internal data class TradeEntry(
        val id: TradeId,
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
        val fees: String,
    )
}
