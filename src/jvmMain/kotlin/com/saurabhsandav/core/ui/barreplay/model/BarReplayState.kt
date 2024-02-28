package com.saurabhsandav.core.ui.barreplay.model

import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import kotlinx.datetime.Instant

internal data class BarReplayState(
    val replayState: ReplayState?,
    val eventSink: (BarReplayEvent) -> Unit,
) {

    internal sealed class ReplayState {

        data class NewReplay(
            val model: NewReplayFormModel,
        ) : ReplayState()

        data class ReplayStarted(
            val replayParams: ReplayParams,
        ) : ReplayState()
    }

    data class ReplayParams(
        val baseTimeframe: Timeframe,
        val candlesBefore: Int,
        val replayFrom: Instant,
        val dataTo: Instant,
        val replayFullBar: Boolean,
        val initialTicker: String,
    )
}
