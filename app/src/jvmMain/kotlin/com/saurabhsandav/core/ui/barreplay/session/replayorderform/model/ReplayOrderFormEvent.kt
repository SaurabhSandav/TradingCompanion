package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormState.QuantityActiveField

internal sealed class ReplayOrderFormEvent {

    data class SetQuantityActiveField(
        val activeField: QuantityActiveField,
    ) : ReplayOrderFormEvent()

    data object Submit : ReplayOrderFormEvent()
}
