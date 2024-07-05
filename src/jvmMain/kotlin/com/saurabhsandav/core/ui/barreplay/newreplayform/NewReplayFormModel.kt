package com.saurabhsandav.core.ui.barreplay.newreplayform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.utils.nowIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class NewReplayFormModel(
    coroutineScope: CoroutineScope,
    baseTimeframe: Timeframe?,
    candlesBefore: String,
    replayFrom: LocalDateTime,
    dataTo: LocalDateTime,
    replayFullBar: Boolean,
    initialTicker: String?,
    profileId: ProfileId?,
    onSubmit: suspend NewReplayFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val baseTimeframeField = validator.addField(baseTimeframe) { isRequired() }

    val candlesBeforeField = validator.addField(candlesBefore) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val replayFromField = validator.addField(replayFrom) {
        isRequired()

        validate(
            isValid = this < Clock.System.nowIn(TimeZone.currentSystemDefault()),
            errorMessage = { "Cannot be in the future" },
        )
    }

    val dataToField = validator.addField(dataTo) {
        isRequired()

        validate(
            isValid = validated(replayFromField) < this,
            errorMessage = { "Cannot be before or same as replay from" },
        )
    }

    var replayFullBar by mutableStateOf(replayFullBar)

    val initialTickerField = validator.addField(initialTicker) { isRequired() }

    val profileField = validator.addField(profileId)
}
