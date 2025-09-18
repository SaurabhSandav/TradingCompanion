package com.saurabhsandav.core.ui.barreplay.newreplayform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.utils.nowIn
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class NewReplayFormModel(
    baseTimeframe: Timeframe?,
    candlesBefore: String,
    replayFrom: LocalDateTime,
    dataTo: LocalDateTime,
    replayFullBar: Boolean,
    initialSymbolId: SymbolId?,
    profileId: ProfileId?,
) : FormModel() {

    val baseTimeframeField = addMutableStateField(baseTimeframe) { isRequired() }

    val candlesBeforeField = addMutableStateField(candlesBefore) {
        isRequired()
        isInt()?.isPositive()
    }

    val replayFromField = addMutableStateField(replayFrom) {
        isRequired()
        if (this >= Clock.System.nowIn(TimeZone.currentSystemDefault())) reportInvalid("Cannot be in the future")
    }

    val dataToField = addMutableStateField(dataTo) {
        isRequired()
        if (this <= replayFromField.validatedValue()) reportInvalid("Cannot be before or same as replay from")
    }

    var replayFullBar by mutableStateOf(replayFullBar)

    val initialSymbolField = addMutableStateField(initialSymbolId) { isRequired() }

    val profileField = addMutableStateField(profileId)
}
