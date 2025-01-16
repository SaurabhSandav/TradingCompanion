package com.saurabhsandav.core.ui.tradeexecutions.model

import androidx.paging.PagingData
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import kotlinx.coroutines.flow.Flow

internal data class TradeExecutionsState(
    val executionEntries: Flow<PagingData<TradeExecutionEntry>>,
    val selectionManager: SelectionManager<TradeExecutionId>,
    val eventSink: (TradeExecutionsEvent) -> Unit,
) {

    internal sealed class TradeExecutionEntry {

        data class Section(
            val isToday: Boolean,
            val count: Flow<Long>,
        ) : TradeExecutionEntry()

        data class Item(
            val id: TradeExecutionId,
            val broker: String,
            val ticker: String,
            val quantity: String,
            val side: String,
            val price: String,
            val timestamp: String,
            val locked: Boolean,
        ) : TradeExecutionEntry()
    }
}
