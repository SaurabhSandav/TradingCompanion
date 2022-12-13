package ui.barreplay

import AppModule
import androidx.compose.runtime.*
import ui.barreplay.charts.ReplayChartsScreen
import ui.barreplay.launchform.ReplayLaunchFormFields
import ui.barreplay.launchform.ReplayLaunchFormScreen
import ui.barreplay.model.BarReplayEvent
import ui.barreplay.model.BarReplayScreen

@Composable
internal fun BarReplayScreen(appModule: AppModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { BarReplayPresenter(scope) }
    val state by presenter.state.collectAsState()

    BarReplayScreen(
        appModule = appModule,
        currentScreen = state.currentScreen,
        onLaunchReplay = { presenter.event(BarReplayEvent.LaunchReplay(it)) },
        onNewReplay = { presenter.event(BarReplayEvent.NewReplay) },
    )
}

@Composable
private fun BarReplayScreen(
    appModule: AppModule,
    currentScreen: BarReplayScreen,
    onLaunchReplay: (ReplayLaunchFormFields.Model) -> Unit,
    onNewReplay: () -> Unit,
) {

    when (currentScreen) {
        is BarReplayScreen.LaunchForm -> ReplayLaunchFormScreen(
            formModel = currentScreen.formModel,
            onLaunchReplay = onLaunchReplay,
        )

        is BarReplayScreen.Chart -> ReplayChartsScreen(
            appModule = appModule,
            onNewReplay = onNewReplay,
            baseTimeframe = currentScreen.baseTimeframe,
            candlesBefore = currentScreen.candlesBefore,
            replayFrom = currentScreen.replayFrom,
            dataTo = currentScreen.dataTo,
            replayFullBar = currentScreen.replayFullBar,
            initialSymbol = currentScreen.initialSymbol,
        )
    }
}
