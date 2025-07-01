package com.saurabhsandav.core.ui.barreplay.model

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayFormModel
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.serialization.Serializable
import kotlin.time.Instant

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

    @Serializable
    data class ReplayParams(
        val baseTimeframe: Timeframe,
        val candlesBefore: Int,
        val replayFrom: Instant,
        val dataTo: Instant,
        val replayFullBar: Boolean,
        val initialTicker: String,
        val profileId: ProfileId?,
    )
}
