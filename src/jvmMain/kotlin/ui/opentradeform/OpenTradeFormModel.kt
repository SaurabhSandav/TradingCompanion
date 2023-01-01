package ui.opentradeform

import androidx.compose.runtime.Stable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.common.form.*

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

    val ticker = validator.newField(
        initial = ticker,
        validations = setOf(IsNotNull),
    )

    val quantity = validator.newField(
        initial = quantity,
        validations = setOf(
            IsNotEmpty,
            IsInt,
            Validation("Cannot be 0 or negative") { it.toInt() > 0 },
        ),
    )

    val isLong = validator.newField(isLong)

    val entry = validator.newField(
        initial = entry,
        validations = setOf(IsNotEmpty, IsBigDecimal),
    )

    val stop = validator.newField(
        initial = stop,
        dependsOn = setOf(this.isLong, this.entry),
        validations = setOf(
            IsNotEmpty,
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid stop",
                validateDependencies = true,
            ) {
                val current = it.toBigDecimal()
                val entryBD = this.entry.value.toBigDecimal()
                if (this.isLong.value) current < entryBD else current > entryBD
            },
        ),
    )

    val entryDateTime = validator.newField(
        initial = entryDateTime,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )

    val target = validator.newField(
        initial = target,
        dependsOn = setOf(this.isLong, this.entry),
        validations = setOf(
            IsNotEmpty,
            IsBigDecimal,
            Validation(
                errorMessage = "Invalid target",
                validateDependencies = true,
            ) {
                val current = it.toBigDecimal()
                val entryBD = this.entry.value.toBigDecimal()
                if (this.isLong.value) current > entryBD else current < entryBD
            },
        ),
    )
}
