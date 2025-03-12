package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
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
    val screensModule = LocalScreensModule.current
    val module = remember { screensModule.barReplayModule(scope) }
    val presenter = remember { module.presenter() }
    val state by presenter.state.collectAsState()

    val replayState = state.replayState ?: return

    AppWindow(
        title = "Bar Replay",
        onCloseRequest = onCloseRequest,
    ) {

        when (replayState) {
            is NewReplay -> NewReplayForm(
                model = replayState.model,
            )

            is ReplayStarted -> ReplaySessionScreen(
                barReplayModule = module,
                onOpenProfile = { replayState.replayParams.profileId?.let(onOpenProfile) },
                onNewReplay = { state.eventSink(BarReplayEvent.NewReplay) },
                replayParams = replayState.replayParams,
            )
        }
    }
}
