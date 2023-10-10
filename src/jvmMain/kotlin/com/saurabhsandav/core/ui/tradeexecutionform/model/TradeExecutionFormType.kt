package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.ui.common.form.FormValidator

internal sealed class TradeExecutionFormType {

    data class New(val formModel: ((FormValidator) -> TradeExecutionFormModel)? = null) : TradeExecutionFormType()

    data class NewFromExisting(val id: Long) : TradeExecutionFormType()

    data class NewFromExistingInTrade(val id: Long) : TradeExecutionFormType()

    data class AddToTrade(val tradeId: Long) : TradeExecutionFormType()

    data class CloseTrade(val tradeId: Long) : TradeExecutionFormType()

    data class Edit(val id: Long) : TradeExecutionFormType()
}
