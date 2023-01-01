package ui.barreplay.model

internal sealed class BarReplayEvent {

    object LaunchReplay : BarReplayEvent()

    object NewReplay : BarReplayEvent()
}
