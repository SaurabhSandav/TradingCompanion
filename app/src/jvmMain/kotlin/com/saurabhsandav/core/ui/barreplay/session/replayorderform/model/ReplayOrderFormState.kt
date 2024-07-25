package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.CoroutineScope

internal data class ReplayOrderFormState(
    val title: String,
    val ticker: String,
    val formModel: ReplayOrderFormModel?,
)

internal class ReplayOrderFormModel(
    coroutineScope: CoroutineScope,
    initial: Initial,
    onSubmit: suspend ReplayOrderFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val quantityField = validator.addField(initial.quantity) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val isBuyField = validator.addField(initial.isBuy)

    val priceField = validator.addField(initial.price) {
        isRequired()
        isBigDecimal {
            isPositive()
        }
    }

    val stop = validator.addField(initial.stop) {
        isBigDecimal {
            isPositive()

            validate(
                isValid = when {
                    validated(isBuyField) -> this < validated(priceField).toBigDecimal()
                    else -> this > validated(priceField).toBigDecimal()
                },
                errorMessage = { "Invalid Stop" },
            )
        }
    }

    val target = validator.addField(initial.target) {
        isBigDecimal {
            isPositive()

            validate(
                isValid = when {
                    validated(isBuyField) -> this > validated(priceField).toBigDecimal()
                    else -> this < validated(priceField).toBigDecimal()
                },
                errorMessage = { "Invalid Target" },
            )
        }
    }

    class Initial(
        val quantity: String = "",
        val isBuy: Boolean = true,
        val price: String = "",
        val stop: String = "",
        val target: String = "",
    )
}
