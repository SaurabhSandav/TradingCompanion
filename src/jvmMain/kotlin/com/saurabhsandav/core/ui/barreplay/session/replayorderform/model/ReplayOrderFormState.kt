package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired

@Immutable
internal data class ReplayOrderFormState(
    val title: String,
    val ticker: String,
    val formModel: ReplayOrderFormModel?,
    val onSaveOrder: () -> Unit,
)

@Stable
internal class ReplayOrderFormModel(
    val validator: FormValidator,
    initial: Initial,
) {

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

            check(
                value = when {
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

            check(
                value = when {
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
