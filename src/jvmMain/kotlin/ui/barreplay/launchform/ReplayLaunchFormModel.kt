package ui.barreplay.launchform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.common.form.*

class ReplayLaunchFormModel(
    validator: FormValidator,
    baseTimeframe: String?,
    candlesBefore: String,
    replayFrom: LocalDateTime,
    dataTo: LocalDateTime,
    replayFullBar: Boolean,
    initialSymbol: String?,
) {

    val baseTimeframe = validator.newField(
        initial = baseTimeframe,
        validations = setOf(IsNotNull),
    )

    val candlesBefore = validator.newField(
        initial = candlesBefore,
        validations = setOf(
            IsNotEmpty,
            IsInt,
            Validation(
                errorMessage = "Cannot be 0 or negative",
                isValid = { it.toInt() > 0 },
            ),
        ),
    )

    val replayFrom = validator.newField(
        initial = replayFrom,
        validations = setOf(
            Validation(
                errorMessage = "Cannot be in the future",
                isValid = { it < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) },
            ),
        ),
    )

    val dataTo = validator.newField(
        initial = dataTo,
        dependsOn = setOf(this.replayFrom),
        validations = setOf(
            Validation(
                errorMessage = "Cannot be before entry time",
                isValid = { this.replayFrom.value < it },
            ),
        ),
    )

    var replayFullBar by mutableStateOf(replayFullBar)

    val initialSymbol = validator.newField(
        initial = initialSymbol,
        validations = setOf(IsNotNull),
    )
}
