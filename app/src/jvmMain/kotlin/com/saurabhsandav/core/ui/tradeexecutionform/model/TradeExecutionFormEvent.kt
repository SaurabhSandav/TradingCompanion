package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormState.QuantityActiveField

internal sealed class TradeExecutionFormEvent {

    data class SetQuantityActiveField(
        val activeField: QuantityActiveField,
    ) : TradeExecutionFormEvent()

    data object Submit : TradeExecutionFormEvent()
}
