package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.adapter.addTextFieldStateField
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.kbigdecimal.toKBigDecimal

internal data class ReplayOrderFormState(
    val title: String,
    val ticker: String,
    val formModel: ReplayOrderFormModel?,
    val onSubmit: () -> Unit,
)

internal class ReplayOrderFormModel(
    quantity: String = "",
    lots: String = "",
    isBuy: Boolean = true,
    price: String = "",
    stop: String = "",
    target: String = "",
) : FormModel() {

    val quantityField = addTextFieldStateField(quantity) {
        isRequired()
        isInt()?.isPositive()
    }

    val lotsField = addTextFieldStateField(lots) {
        isRequired(false)
        isInt()?.isPositive()
    }

    val isBuyField = addMutableStateField(isBuy)

    val priceField = addTextFieldStateField(price) {
        isRequired()
        isBigDecimal()?.isPositive()
    }

    val stop = addTextFieldStateField(stop) {
        isRequired(false)
        isBigDecimal()?.apply {
            isPositive()

            val isValid = when {
                isBuyField.validatedValue() -> this < priceField.validatedValue().toKBigDecimal()
                else -> this > priceField.validatedValue().toKBigDecimal()
            }

            if (!isValid) reportInvalid("Invalid Stop")
        }
    }

    val target = addTextFieldStateField(target) {
        isRequired(false)
        isBigDecimal()?.apply {
            isPositive()

            val isValid = when {
                isBuyField.validatedValue() -> this > priceField.validatedValue().toKBigDecimal()
                else -> this < priceField.validatedValue().toKBigDecimal()
            }

            if (!isValid) reportInvalid("Invalid Target")
        }
    }
}
