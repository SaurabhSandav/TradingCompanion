package com.saurabhsandav.core.ui.tradeexecutionform.model

import java.math.BigDecimal

internal sealed class TradeExecutionFormType {

    data object New : TradeExecutionFormType()

    data class NewFromExisting(val id: Long) : TradeExecutionFormType()

    data class NewFromExistingInTrade(val id: Long) : TradeExecutionFormType()

    data class NewSized(
        val initialModel: TradeExecutionFormModel.Initial,
        val stop: BigDecimal,
        val target: BigDecimal,
    ) : TradeExecutionFormType()

    data class AddToTrade(val tradeId: Long) : TradeExecutionFormType()

    data class CloseTrade(val tradeId: Long) : TradeExecutionFormType()

    data class Edit(val id: Long) : TradeExecutionFormType()
}
