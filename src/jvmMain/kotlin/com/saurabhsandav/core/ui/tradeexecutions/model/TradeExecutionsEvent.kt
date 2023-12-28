package com.saurabhsandav.core.ui.tradeexecutions.model

import com.saurabhsandav.core.trades.model.TradeExecutionId

internal sealed class TradeExecutionsEvent {

    data object NewExecution : TradeExecutionsEvent()

    data class NewExecutionFromExisting(val id: TradeExecutionId) : TradeExecutionsEvent()

    data class EditExecution(val id: TradeExecutionId) : TradeExecutionsEvent()

    data class LockExecutions(val ids: List<TradeExecutionId>) : TradeExecutionsEvent()

    data class DeleteExecutions(val ids: List<TradeExecutionId>) : TradeExecutionsEvent()
}
