package com.saurabhsandav.core.ui.barreplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    AppWindow(
        title = "Bar Replay",
        onCloseRequest = onCloseRequest,
    ) {

        Box(Modifier.wrapContentSize()) {

            when (val replayState = state.replayState) {
                null -> CircularProgressIndicator()
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
}
