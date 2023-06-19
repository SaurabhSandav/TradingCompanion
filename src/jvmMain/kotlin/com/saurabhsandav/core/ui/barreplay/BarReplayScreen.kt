package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.barreplay.model.BarReplayEvent
import com.saurabhsandav.core.ui.barreplay.model.BarReplayScreen
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

    when (val currentScreen = state.currentScreen) {
        is BarReplayScreen.LaunchForm -> {

            AppWindow(
                state = windowState,
                title = "Bar Replay",
                onCloseRequest = onCloseRequest,
            ) {

                NewReplayForm(
                    model = currentScreen.model,
                    onLaunchReplay = { presenter.event(BarReplayEvent.LaunchReplay) },
                )
            }
        }

        is BarReplayScreen.Chart -> ReplaySessionScreen(
            onNewReplay = { presenter.event(BarReplayEvent.NewReplay) },
            baseTimeframe = currentScreen.baseTimeframe,
            candlesBefore = currentScreen.candlesBefore,
            replayFrom = currentScreen.replayFrom,
            dataTo = currentScreen.dataTo,
            replayFullBar = currentScreen.replayFullBar,
            initialTicker = currentScreen.initialTicker,
        )
    }
}
