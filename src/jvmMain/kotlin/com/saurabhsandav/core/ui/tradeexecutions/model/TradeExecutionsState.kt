package com.saurabhsandav.core.ui.tradeexecutions.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage

@Immutable
internal data class TradeExecutionsState(
    val executionsList: ExecutionsList,
    val selectionManager: SelectionManager<TradeExecutionId>,
    val canSelectionLock: Boolean,
    val errors: List<UIErrorMessage>,
    val eventSink: (TradeExecutionsEvent) -> Unit,
) {

    @Immutable
    internal data class ExecutionsList(
        val todayExecutions: List<TradeExecutionEntry>,
        val pastExecutions: List<TradeExecutionEntry>,
    )

    @Immutable
    internal data class TradeExecutionEntry(
        val id: TradeExecutionId,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )
}
