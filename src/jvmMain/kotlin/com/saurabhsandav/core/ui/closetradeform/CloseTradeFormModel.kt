package com.saurabhsandav.core.ui.closetradeform

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

@Stable
class CloseTradeFormModel(
    validator: FormValidator,
    ticker: String?,
    quantity: String,
    isLong: Boolean,
    entry: String,
    stop: String,
    entryDateTime: LocalDateTime,
    target: String,
    exit: String,
    exitDateTime: LocalDateTime,
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

    val exit = validator.textField(
        initial = exit,
        validations = setOf(IsBigDecimal),
    )

    val exitDateTime = validator.dateTimeField(
        initial = exitDateTime,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be before entry time",
                dependsOn = setOf(this.entryDateTime),
                isValid = { this.entryDateTime.value < it },
            ),
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )
}
