package com.saurabhsandav.core.ui.tradeexecutions.model

import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage

internal data class TradeExecutionsState(
    val executionsList: ExecutionsList,
    val selectionManager: SelectionManager<TradeExecutionId>,
    val canSelectionLock: Boolean,
    val errors: List<UIErrorMessage>,
    val eventSink: (TradeExecutionsEvent) -> Unit,
) {

    internal data class ExecutionsList(
        val todayExecutions: List<TradeExecutionEntry>,
        val pastExecutions: List<TradeExecutionEntry>,
    )

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
