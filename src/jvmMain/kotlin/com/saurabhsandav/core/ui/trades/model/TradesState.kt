package com.saurabhsandav.core.ui.trades.model

import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.coroutines.flow.Flow

internal data class TradesState(
    val tradesList: TradesList,
    val errors: List<UIErrorMessage>,
    val eventSink: (TradesEvent) -> Unit,
) {

    sealed class TradesList {

        data class All(
            val trades: List<TradeEntry>,
            val isFiltered: Boolean,
        ) : TradesList()

        data class Focused(
            val openTrades: List<TradeEntry>,
            val todayTrades: List<TradeEntry>,
            val todayStats: Stats?,
            val pastTrades: List<TradeEntry>,
        ) : TradesList()
    }

    internal data class Stats(
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )

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
