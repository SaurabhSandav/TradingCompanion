package com.saurabhsandav.core.ui.tradeexecutionform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.IsBigDecimal
import com.saurabhsandav.core.ui.common.form.IsInt
import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.fields.*
import kotlinx.datetime.*

@Immutable
internal data class TradeExecutionFormState(
    val title: String,
    val formModel: TradeExecutionFormModel?,
    val onSaveExecution: () -> Unit,
)

@Stable
internal class TradeExecutionFormModel(
    validator: FormValidator,
    instrument: Instrument?,
    ticker: String?,
    quantity: String,
    lots: String,
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

    val date = validator.dateField(
        initial = timestamp.date,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { date ->
                    date <= Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                },
            ),
        ),
    )

    val time = validator.timeField(
        initial = timestamp.time,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                dependsOn = setOf(date),
                isValid = { time ->
                    date.value.atTime(time) < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                },
            ),
        ),
    )

    val timestamp: LocalDateTime
        get() = date.value.atTime(time.value)
}
