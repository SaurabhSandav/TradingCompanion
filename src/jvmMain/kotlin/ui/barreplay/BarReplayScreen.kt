package ui.barreplay

import AppModule
import androidx.compose.runtime.*
import kotlinx.datetime.*
import ui.barreplay.model.BarReplayEvent
import ui.barreplay.model.BarReplayFormFields
import ui.barreplay.model.BarReplayScreen
import ui.barreplay.ui.ReplayChart
import ui.barreplay.ui.ReplayLaunchForm
import ui.common.form.rememberFormScope

@Composable
internal fun BarReplayScreen(appModule: AppModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { BarReplayPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    BarReplayScreen(
        currentScreen = state.currentScreen,
        onLaunchReplay = { presenter.event(BarReplayEvent.LaunchReplay(it)) },
        areReplayControlsEnabled = state.areReplayControlsEnabled,
        onNewReplay = { presenter.event(BarReplayEvent.NewReplay) },
        onReset = { presenter.event(BarReplayEvent.Reset) },
        onNext = { presenter.event(BarReplayEvent.Next) },
        onSymbolChange = { presenter.event(BarReplayEvent.ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(BarReplayEvent.ChangeTimeframe(it)) },
        onIsAutoNextEnabledChange = { presenter.event(BarReplayEvent.ChangeIsAutoNextEnabled(it)) },
    )
}

@Composable
private fun BarReplayScreen(
    currentScreen: BarReplayScreen,
    onLaunchReplay: (BarReplayFormFields) -> Unit,
    areReplayControlsEnabled: Boolean,
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    val formScope = rememberFormScope()

    val fields = remember {
        BarReplayFormFields(
            formScope = formScope,
            initial = BarReplayFormFields.Model(
                symbol = null,
                timeframe = null,
                dataFrom = (Clock.System.now().minus(
                    30,
                    DateTimeUnit.DAY,
                    TimeZone.currentSystemDefault()
                )).toLocalDateTime(TimeZone.currentSystemDefault()),
                dataTo = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                replayFrom = (Clock.System.now().minus(
                    15,
                    DateTimeUnit.DAY,
                    TimeZone.currentSystemDefault()
                )).toLocalDateTime(TimeZone.currentSystemDefault()),
            ),
        )
    }

    when (currentScreen) {
        BarReplayScreen.LaunchForm -> ReplayLaunchForm(fields) {
            if (formScope.isFormValid())
                onLaunchReplay(fields)
        }

        is BarReplayScreen.Chart -> ReplayChart(
            fields = fields,
            chartState = currentScreen.chartState,
            areReplayControlsEnabled = areReplayControlsEnabled,
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
        )
    }
}
