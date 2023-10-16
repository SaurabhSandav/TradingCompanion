package com.saurabhsandav.core.ui.tradeexecutionform.model

internal sealed class TradeExecutionFormType {

    data class New(val initialModel: TradeExecutionFormModel.Initial? = null) : TradeExecutionFormType()

    data class NewFromExisting(val id: Long) : TradeExecutionFormType()

    data class NewFromExistingInTrade(val id: Long) : TradeExecutionFormType()

    data class AddToTrade(val tradeId: Long) : TradeExecutionFormType()

    data class CloseTrade(val tradeId: Long) : TradeExecutionFormType()

    data class Edit(val id: Long) : TradeExecutionFormType()
}
