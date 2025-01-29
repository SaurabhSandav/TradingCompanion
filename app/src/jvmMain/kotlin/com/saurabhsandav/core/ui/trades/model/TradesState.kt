package com.saurabhsandav.core.ui.trades.model

import androidx.paging.PagingData
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.SelectionManager
import kotlinx.coroutines.flow.Flow

internal data class TradesState(
    val tradeEntries: Flow<PagingData<TradeEntry>>,
    val isFocusModeEnabled: Boolean,
    val selectionManager: SelectionManager<TradeId>,
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
            val duration: Duration,
            val pnl: String,
            val isProfitable: Boolean,
            val netPnl: String,
            val isNetProfitable: Boolean,
            val fees: String,
        ) : TradeEntry() {

            sealed class Duration {

                data class Open(val flow: Flow<String>) : Duration()

                data class Closed(val str: String) : Duration()
            }
        }
    }

    internal data class Stats(
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
    )
}
