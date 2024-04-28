package com.saurabhsandav.core.ui.trades.model

import app.cash.paging.PagingData
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.coroutines.flow.Flow

internal data class TradesState(
    val tradeEntries: Flow<PagingData<TradeEntry>>,
    val isFocusModeEnabled: Boolean,
    val errors: List<UIErrorMessage>,
    val eventSink: (TradesEvent) -> Unit,
) {

    internal sealed class TradeEntry {

        data class Section(
            val type: Type,
            val count: Flow<Long>,
            val stats: Flow<Stats>? = null,
        ) : TradeEntry() {

            enum class Type {
                Open,
                Today,
                Past,
                All,
                Filtered,
            }
        }

        internal data class Item(
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
        ) : TradeEntry()
    }

    internal data class Stats(
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )
}
