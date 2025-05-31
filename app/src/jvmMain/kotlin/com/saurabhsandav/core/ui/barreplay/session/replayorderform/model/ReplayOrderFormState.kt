package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class ReplayOrderFormState(
    val title: String,
    val ticker: String,
    val formModel: ReplayOrderFormModel?,
    val onSubmit: () -> Unit,
)

internal class ReplayOrderFormModel(
    quantity: String = "",
    isBuy: Boolean = true,
    price: String = "",
    stop: String = "",
    target: String = "",
) : FormModel() {

    val quantityField = addField(quantity) {
        isRequired()
        isInt()?.isPositive()
    }

    val isBuyField = addField(isBuy)

    val priceField = addField(price) {
        isRequired()
        isBigDecimal()?.isPositive()
    }

    val stop = addField(stop) {
        isRequired(false)
        isBigDecimal()?.apply {
            isPositive()

            val isValid = when {
                isBuyField.validatedValue() -> this < priceField.validatedValue().toBigDecimal()
                else -> this > priceField.validatedValue().toBigDecimal()
            }

            if (!isValid) reportInvalid("Invalid Stop")
        }
    }

    val target = addField(target) {
        isRequired(false)
        isBigDecimal()?.apply {
            isPositive()

            val isValid = when {
                isBuyField.validatedValue() -> this > priceField.validatedValue().toBigDecimal()
                else -> this < priceField.validatedValue().toBigDecimal()
            }

            if (!isValid) reportInvalid("Invalid Target")
        }
    }
}
