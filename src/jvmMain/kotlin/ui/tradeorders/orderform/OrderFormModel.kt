package ui.tradeorders.orderform

import androidx.compose.runtime.Stable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.common.form.FormValidator
import ui.common.form.IsBigDecimal
import ui.common.form.IsInt
import ui.common.form.Validation
import ui.common.form.fields.dateTimeField
import ui.common.form.fields.listSelectionField
import ui.common.form.fields.switch
import ui.common.form.fields.textField

@Stable
internal class OrderFormModel(
    validator: FormValidator,
    ticker: String?,
    quantity: String,
    isBuy: Boolean,
    price: String,
    timestamp: LocalDateTime,
) {

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
