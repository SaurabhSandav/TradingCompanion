package com.saurabhsandav.core.ui.tradeorderform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.IsBigDecimal
import com.saurabhsandav.core.ui.common.form.IsInt
import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.fields.dateTimeField
import com.saurabhsandav.core.ui.common.form.fields.listSelectionField
import com.saurabhsandav.core.ui.common.form.fields.switch
import com.saurabhsandav.core.ui.common.form.fields.textField
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Immutable
internal data class OrderFormState(
    val title: String,
    val formModel: OrderFormModel?,
    val onSaveOrder: () -> Unit,
)

@Stable
internal class OrderFormModel(
    validator: FormValidator,
    instrument: String?,
    ticker: String?,
    quantity: String,
    isBuy: Boolean,
    price: String,
    timestamp: LocalDateTime,
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

    val isBuy = validator.switch(isBuy)

    val price = validator.textField(
        initial = price,
        validations = setOf(IsBigDecimal),
    )

    val timestamp = validator.dateTimeField(
        initial = timestamp,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )
}
