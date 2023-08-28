package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.NewReplay
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayState.ReplayStarted
import com.saurabhsandav.core.ui.barreplay.newreplayform.NewReplayForm
import com.saurabhsandav.core.ui.barreplay.session.ReplaySessionScreen
import com.saurabhsandav.core.ui.common.app.AppWindow

@Composable
internal fun BarReplayWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { BarReplayPresenter(scope) }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        title = "Bar Replay",
        onCloseRequest = onCloseRequest,
    ) {

        when (val replayState = state.replayState) {
            is NewReplay -> NewReplayForm(
                model = replayState.model,
                onLaunchReplay = { state.eventSink(BarReplayEvent.LaunchReplay) },
            )

            is ReplayStarted -> ReplaySessionScreen(
                onNewReplay = { state.eventSink(BarReplayEvent.NewReplay) },
                replayParams = replayState.replayParams,
            )
        }
    }
}
