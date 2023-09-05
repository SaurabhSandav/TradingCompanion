package com.saurabhsandav.core.ui.tradeexecutions.model

import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ProfileTradeExecutionId

internal sealed class TradeExecutionsEvent {

    data object NewExecution : TradeExecutionsEvent()

    data class NewExecutionFromExisting(val profileTradeExecutionId: ProfileTradeExecutionId) : TradeExecutionsEvent()

    data class EditExecution(val profileTradeExecutionId: ProfileTradeExecutionId) : TradeExecutionsEvent()

    data class LockExecutions(val ids: List<ProfileTradeExecutionId>) : TradeExecutionsEvent()

    data class DeleteExecutions(val ids: List<ProfileTradeExecutionId>) : TradeExecutionsEvent()
}
