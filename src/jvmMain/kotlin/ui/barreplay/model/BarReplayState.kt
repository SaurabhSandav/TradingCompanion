package ui.barreplay.model

import androidx.compose.runtime.Immutable
import ui.barreplay.ReplayChartState

@Immutable
internal data class BarReplayState(
    val currentScreen: BarReplayScreen,
    val areReplayControlsEnabled: Boolean,
)

internal sealed class BarReplayScreen {

    object LaunchForm : BarReplayScreen()

    data class Chart(val chartState: ReplayChartState) : BarReplayScreen()
}
