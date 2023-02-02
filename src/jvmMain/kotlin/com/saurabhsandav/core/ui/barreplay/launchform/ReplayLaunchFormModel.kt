package com.saurabhsandav.core.ui.barreplay.launchform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.IsInt
import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.fields.dateTimeField
import com.saurabhsandav.core.ui.common.form.fields.listSelectionField
import com.saurabhsandav.core.ui.common.form.fields.textField
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ReplayLaunchFormModel(
    validator: FormValidator,
    baseTimeframe: String?,
    candlesBefore: String,
    replayFrom: LocalDateTime,
    dataTo: LocalDateTime,
    replayFullBar: Boolean,
    initialSymbol: String?,
) {

    val baseTimeframe = validator.listSelectionField(baseTimeframe)

    val candlesBefore = validator.textField(
        initial = candlesBefore,
        validations = setOf(
            IsInt,
            Validation(
                errorMessage = "Cannot be 0 or negative",
                isValid = { it.toInt() > 0 },
            ),
        ),
    )

    val replayFrom = validator.dateTimeField(
        initial = replayFrom,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )

    val dataTo = validator.dateTimeField(
        initial = dataTo,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be before entry time",
                dependsOn = setOf(this.replayFrom),
                isValid = { this.replayFrom.value < it },
            ),
        ),
    )

    var replayFullBar by mutableStateOf(replayFullBar)

    val initialSymbol = validator.listSelectionField(initialSymbol)
}
