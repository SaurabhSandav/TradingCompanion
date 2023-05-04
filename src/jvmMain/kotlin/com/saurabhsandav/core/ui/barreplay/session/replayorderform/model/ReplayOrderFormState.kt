package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.IsBigDecimal
import com.saurabhsandav.core.ui.common.form.IsInt
import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.fields.listSelectionField
import com.saurabhsandav.core.ui.common.form.fields.switch
import com.saurabhsandav.core.ui.common.form.fields.textField

@Immutable
internal data class ReplayOrderFormState(
    val title: String,
    val formModel: ReplayOrderFormModel?,
    val onSaveOrder: () -> Unit,
)

@Stable
internal class ReplayOrderFormModel(
    validator: FormValidator,
    instrument: String?,
    ticker: String?,
    quantity: String,
    lots: String,
    isBuy: Boolean,
    price: String,
    stop: String,
    target: String,
) {

    val instrument = validator.listSelectionField(instrument)

    val ticker = validator.listSelectionField(ticker)

    val quantity = validator.textField(
        initial = quantity,
        validations = setOf(
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
    )

    val lots = validator.textField(
        initial = lots,
        validations = setOf(
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
        isRequired = false,
    )

    val isBuy = validator.switch(isBuy)

    val price = validator.textField(
        initial = price,
        validations = setOf(IsBigDecimal),
    )

    val stop = validator.textField(
        initial = stop,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid Stop",
                dependsOn = setOf(this.price, this.isBuy),
                isValid = { stop ->
                    val stopBD = stop.toBigDecimal()
                    val priceBD = this.price.value.toBigDecimal()
                    if (this.isBuy.value) stopBD < priceBD else stopBD > priceBD
                },
            ),
        ),
        isRequired = false,
    )

    val target = validator.textField(
        initial = target,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid Target",
                dependsOn = setOf(this.price, this.isBuy),
                isValid = { target ->
                    val targetBD = target.toBigDecimal()
                    val priceBD = this.price.value.toBigDecimal()
                    if (this.isBuy.value) targetBD > priceBD else targetBD < priceBD
                },
            ),
        ),
        isRequired = false,
    )
}
