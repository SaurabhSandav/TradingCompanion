package com.saurabhsandav.core.ui.tradeexecutions.model

import com.saurabhsandav.trading.record.model.TradeExecutionId

internal sealed class TradeExecutionsEvent {

    data object NewExecution : TradeExecutionsEvent()

    data class NewExecutionFromExisting(
        val id: TradeExecutionId,
    ) : TradeExecutionsEvent()

    data class EditExecution(
        val id: TradeExecutionId,
    ) : TradeExecutionsEvent()

    data class LockExecutions(
        val ids: List<TradeExecutionId>,
    ) : TradeExecutionsEvent()

    data class DeleteExecutions(
        val ids: List<TradeExecutionId>,
    ) : TradeExecutionsEvent()
}
