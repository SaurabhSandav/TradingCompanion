package com.saurabhsandav.core.ui.barreplay.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import kotlinx.datetime.Instant

@Immutable
internal data class BarReplayState(
    val replayState: ReplayState,
) {

    @Immutable
    internal sealed class ReplayState {

        @Immutable
        data class NewReplay(
            val model: NewReplayFormModel,
        ) : ReplayState()

        @Immutable
        data class ReplayStarted(
            val replayParams: ReplayParams,
        ) : ReplayState()
    }

    @Immutable
    data class ReplayParams(
        val baseTimeframe: Timeframe,
        val candlesBefore: Int,
        val replayFrom: Instant,
        val dataTo: Instant,
        val replayFullBar: Boolean,
        val initialTicker: String,
    )
}
