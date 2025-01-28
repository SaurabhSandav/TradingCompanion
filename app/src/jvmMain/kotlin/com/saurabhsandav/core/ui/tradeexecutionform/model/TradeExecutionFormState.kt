package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.utils.nowIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime

internal data class TradeExecutionFormState(
    val title: String,
    val formModel: TradeExecutionFormModel?,
)

internal class TradeExecutionFormModel(
    coroutineScope: CoroutineScope,
    initial: Initial,
    onSubmit: suspend TradeExecutionFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val instrumentField = validator.addField(initial.instrument) { isRequired() }

    val tickerField = validator.addField(initial.ticker) { isRequired() }

    val quantityField = validator.addField(initial.quantity) {
        isRequired()
        isInt()?.isPositive()
    }

    val lotsField = validator.addField(initial.lots) {
        isRequired(false)
        isInt()?.isPositive()
    }

    val isBuyField = validator.addField(initial.isBuy)

    val priceField = validator.addField(initial.price) {
        isRequired()
        isBigDecimal()?.isPositive()
    }

    val dateField = validator.addField(initial.timestamp.date) {
        if (this > currentLocalDateTime().date) reportInvalid("Cannot be in the future")
    }

    val timeField = validator.addField(initial.timestamp.time) {
        if (validated(dateField).atTime(this) >= currentLocalDateTime()) reportInvalid("Cannot be in the future")
    }

    private fun currentLocalDateTime() = Clock.System.nowIn(TimeZone.currentSystemDefault())

    val timestamp: LocalDateTime
        get() = dateField.value.atTime(timeField.value)

    val addStopField = validator.addField(true)

    val addTargetField = validator.addField(true)

    class Initial(
        val instrument: Instrument? = null,
        val ticker: String? = null,
        val quantity: String = "",
        val lots: String = "",
        val isBuy: Boolean = true,
        val price: String = "",
        val timestamp: LocalDateTime = Clock.System.nowIn(TimeZone.currentSystemDefault()),
    )
}
