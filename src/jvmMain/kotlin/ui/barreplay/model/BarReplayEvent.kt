package ui.barreplay.model

internal sealed class BarReplayEvent {

    data class LaunchReplay(val fields: BarReplayFormFields) : BarReplayEvent()

    object NewReplay : BarReplayEvent()

    object Reset : BarReplayEvent()

    object Next : BarReplayEvent()

    data class ChangeSymbol(val symbol: String) : BarReplayEvent()

    data class ChangeTimeframe(val timeframe: String) : BarReplayEvent()

    data class ChangeIsAutoNextEnabled(val isAutoNextEnabled: Boolean) : BarReplayEvent()
}
