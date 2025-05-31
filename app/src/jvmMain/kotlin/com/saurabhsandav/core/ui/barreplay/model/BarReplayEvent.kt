package com.saurabhsandav.core.ui.barreplay.model

internal sealed class BarReplayEvent {

    data object SubmitReplayForm : BarReplayEvent()

    data object NewReplay : BarReplayEvent()
}
