package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent.SubmitReplayForm
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.NewReplay
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.ReplayStarted
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayForm
import com.saurabhsandav.core.ui.barreplay.session.ReplaySessionScreen
import com.saurabhsandav.core.ui.common.app.AppWindow

@Composable
internal fun BarReplayWindow(
    onCloseRequest: () -> Unit,
    onOpenProfile: (ProfileId) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val graph = remember { appGraph.barReplayGraphFactory.create() }
    val presenter = remember { graph.presenterFactory.create(scope) }
    val state by presenter.state.collectAsState()

    val replayState = state.replayState ?: return

    AppWindow(
        title = "Bar Replay",
        onCloseRequest = onCloseRequest,
    ) {

        when (replayState) {
            is NewReplay -> NewReplayForm(
                model = replayState.model,
                onSubmit = { state.eventSink(SubmitReplayForm) },
            )

            is ReplayStarted -> ReplaySessionScreen(
                barReplayGraph = graph,
                onOpenProfile = { replayState.replayParams.profileId?.let(onOpenProfile) },
                onNewReplay = { state.eventSink(BarReplayEvent.NewReplay) },
                replayParams = replayState.replayParams,
            )
        }
    }
}
