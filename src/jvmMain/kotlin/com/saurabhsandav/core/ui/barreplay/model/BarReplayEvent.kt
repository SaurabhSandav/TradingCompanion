package com.saurabhsandav.core.ui.barreplay.model

internal sealed class BarReplayEvent {

    data object LaunchReplay : BarReplayEvent()

    data object NewReplay : BarReplayEvent()
}
