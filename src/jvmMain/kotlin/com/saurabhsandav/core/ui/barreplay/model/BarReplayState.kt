package com.saurabhsandav.core.ui.barreplay.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import kotlinx.datetime.Instant

@Immutable
internal data class BarReplayState(
    val currentScreen: BarReplayScreen,
)

internal sealed class BarReplayScreen {

    data class LaunchForm(
        val model: NewReplayFormModel,
    ) : BarReplayScreen()

    data class Chart(
        val baseTimeframe: Timeframe,
        val candlesBefore: Int,
        val replayFrom: Instant,
        val dataTo: Instant,
        val replayFullBar: Boolean,
        val initialTicker: String,
    ) : BarReplayScreen()
}
