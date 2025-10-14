package com.saurabhsandav.core.ui.tradeexecutionform.model

import com.saurabhsandav.core.CachedSymbol
import com.saurabhsandav.core.trading.isValidPrice
import com.saurabhsandav.core.trading.isValidQuantity
import com.saurabhsandav.core.ui.common.controls.TimeFieldDefaults
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.adapter.addTextFieldStateField
import com.saurabhsandav.core.ui.common.form.finishValidation
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.utils.nowIn
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlin.time.Clock

internal data class TradeExecutionFormState(
    val title: String,
    val isSymbolEditable: Boolean,
    val isSideSelectable: Boolean,
    val formModel: TradeExecutionFormModel,
    val onSubmit: () -> Unit,
)

internal class TradeExecutionFormModel(
    getSymbol: suspend (SymbolId) -> CachedSymbol,
    symbolId: SymbolId? = null,
    quantity: String = "",
    lots: String = "",
    isBuy: Boolean = true,
    price: String = "",
    timestamp: LocalDateTime = Clock.System.nowIn(TimeZone.currentSystemDefault()),
) : FormModel() {

    val symbolField = addMutableStateField(symbolId) { isRequired() }

    val quantityField = addTextFieldStateField(quantity) {
        isRequired()
        val quantity = isBigDecimal()?.isPositive()!!

        val symbolId = symbolField.validatedValue()!!
        val lotSize = getSymbol(symbolId).lotSize

        if (!isValidQuantity(quantity, lotSize)) reportInvalid("Lot Size: $lotSize")
    }

    val lotsField = addTextFieldStateField(lots) {
        isRequired(false)
        isInt()?.isPositive()
    }

    val isBuyField = addMutableStateField(isBuy)

    val priceField = addTextFieldStateField(price) {
        isRequired()
        val price = isBigDecimal()?.isPositive()!!

        val symbolId = symbolField.validatedValue()!!
        val tickSize = getSymbol(symbolId).tickSize

        if (!isValidPrice(price, tickSize)) reportInvalid("Tick Size: $tickSize")
    }

    val dateField = addMutableStateField(timestamp.date) {
        if (this > currentLocalDateTime().date) reportInvalid("Cannot be in the future")
    }

    val timeField = addTextFieldStateField(TimeFieldDefaults.format(timestamp.time)) {
        isRequired()

        val time = with(TimeFieldDefaults) { validate() } ?: finishValidation()

        if (dateField.validatedValue().atTime(time) >= currentLocalDateTime()) reportInvalid("Cannot be in the future")
    }

    private fun currentLocalDateTime() = Clock.System.nowIn(TimeZone.currentSystemDefault())

    val timestamp: LocalDateTime
        get() = dateField.value.atTime(TimeFieldDefaults.parse(timeField.value))

    val addStopField = addMutableStateField(true)

    val addTargetField = addMutableStateField(true)
}
