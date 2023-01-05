package ui.opentradeform

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
internal class OpenTradeFormModel(
    validator: FormValidator,
    ticker: String?,
    quantity: String,
    isLong: Boolean,
    entry: String,
    stop: String,
    entryDateTime: LocalDateTime,
    target: String,
) {

    val ticker = validator.listSelectionField(ticker)

    val quantity = validator.textField(
        initial = quantity,
        validations = setOf(
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
    )

    val isLong = validator.switch(isLong)

    val entry = validator.textField(
        initial = entry,
        validations = setOf(IsBigDecimal),
    )

    val stop = validator.textField(
        initial = stop,
        isRequired = false,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid stop",
                dependsOn = setOf(this.isLong, this.entry),
            ) {
                val current = it.toBigDecimal()
                val entryBD = this.entry.value.toBigDecimal()
                if (this.isLong.value) current < entryBD else current > entryBD
            },
        ),
    )

    val entryDateTime = validator.dateTimeField(
        initial = entryDateTime,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )

    val target = validator.textField(
        initial = target,
        isRequired = false,
        validations = setOf(
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid target",
                dependsOn = setOf(this.isLong, this.entry),
            ) {
                val current = it.toBigDecimal()
                val entryBD = this.entry.value.toBigDecimal()
                if (this.isLong.value) current > entryBD else current < entryBD
            },
        ),
    )
}
