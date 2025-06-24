package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.trading.record.model.Instrument
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.utils.nowIn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlin.time.Clock

internal data class TradeExecutionFormState(
    val title: String,
    val formModel: TradeExecutionFormModel?,
    val onSubmit: () -> Unit,
)

internal class TradeExecutionFormModel(
    instrument: Instrument? = null,
    ticker: String? = null,
    quantity: String = "",
    lots: String = "",
    isBuy: Boolean = true,
    price: String = "",
    timestamp: LocalDateTime = Clock.System.nowIn(TimeZone.currentSystemDefault()),
) : FormModel() {

    val instrumentField = addField(instrument) { isRequired() }

    val tickerField = addField(ticker) { isRequired() }

    val quantityField = addField(quantity) {
        isRequired()
        isInt()?.isPositive()
    }

    val lotsField = addField(lots) {
        isRequired(false)
        isInt()?.isPositive()
    }

    val isBuyField = addField(isBuy)

    val priceField = addField(price) {
        isRequired()
        isBigDecimal()?.isPositive()
    }

    val dateField = addField(timestamp.date) {
        if (this > currentLocalDateTime().date) reportInvalid("Cannot be in the future")
    }

    val timeField = addField(timestamp.time) {
        if (dateField.validatedValue().atTime(this) >= currentLocalDateTime()) reportInvalid("Cannot be in the future")
    }

    private fun currentLocalDateTime() = Clock.System.nowIn(TimeZone.currentSystemDefault())

    val timestamp: LocalDateTime
        get() = dateField.value.atTime(timeField.value)

    val addStopField = addField(true)

    val addTargetField = addField(true)
}
