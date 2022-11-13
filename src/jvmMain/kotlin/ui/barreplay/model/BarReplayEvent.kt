package ui.barreplay.model

import ui.barreplay.launchform.ReplayLaunchFormFields

internal sealed class BarReplayEvent {

    data class LaunchReplay(val formModel: ReplayLaunchFormFields.Model) : BarReplayEvent()

    object NewReplay : BarReplayEvent()
}
