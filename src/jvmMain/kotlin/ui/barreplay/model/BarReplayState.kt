package ui.barreplay.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant
import trading.Timeframe
import ui.barreplay.launchform.ReplayLaunchFormModel

@Immutable
internal data class BarReplayState(
    val currentScreen: BarReplayScreen,
)

internal sealed class BarReplayScreen {

    data class LaunchForm(
        val model: ReplayLaunchFormModel,
    ) : BarReplayScreen()

    data class Chart(
        val baseTimeframe: Timeframe,
        val candlesBefore: Int,
        val replayFrom: Instant,
        val dataTo: Instant,
        val replayFullBar: Boolean,
        val initialSymbol: String,
    ) : BarReplayScreen()
}
