package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeId
import java.math.BigDecimal

internal sealed class TradeExecutionFormType {

    data object New : TradeExecutionFormType()

    data class NewFromExisting(
        val id: TradeExecutionId,
    ) : TradeExecutionFormType()

    data class NewFromExistingInTrade(
        val id: TradeExecutionId,
    ) : TradeExecutionFormType()

    data class NewSized(
        val formModel: TradeExecutionFormModel,
        val stop: BigDecimal,
        val target: BigDecimal,
    ) : TradeExecutionFormType()

    data class AddToTrade(
        val tradeId: TradeId,
    ) : TradeExecutionFormType()

    data class CloseTrade(
        val tradeId: TradeId,
    ) : TradeExecutionFormType()

    data class Edit(
        val id: TradeExecutionId,
    ) : TradeExecutionFormType()
}
