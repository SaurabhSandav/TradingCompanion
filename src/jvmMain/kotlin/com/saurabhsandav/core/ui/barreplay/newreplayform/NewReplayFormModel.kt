package com.saurabhsandav.core.ui.barreplay.newreplayform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NewReplayFormModel(
    val validator: FormValidator,
    baseTimeframe: Timeframe?,
    candlesBefore: String,
    replayFrom: LocalDateTime,
    dataTo: LocalDateTime,
    replayFullBar: Boolean,
    initialTicker: String?,
) {

    val baseTimeframeField = validator.addField(baseTimeframe) { isRequired() }

    val candlesBeforeField = validator.addField(candlesBefore) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val replayFromField = validator.addField(replayFrom) {
        isRequired()

        check(
            value = this < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            errorMessage = { "Cannot be in the future" },
        )
    }

    val dataToField = validator.addField(dataTo) {
        isRequired()

        check(
            value = validated(replayFromField) < this,
            errorMessage = { "Cannot be before or same as replay from" },
        )
    }

    var replayFullBar by mutableStateOf(replayFullBar)

    val initialTickerField = validator.addField(initialTicker) { isRequired() }
}
