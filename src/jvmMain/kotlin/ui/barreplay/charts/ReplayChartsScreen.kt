package ui.barreplay.charts

import AppModule
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import trading.Timeframe
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.model.ReplayControlsState
import ui.barreplay.charts.ui.ReplayControls
import ui.common.ResizableChart

@Composable
internal fun ReplayChartsScreen(
    appModule: AppModule,
    onNewReplay: () -> Unit,
    baseTimeframe: Timeframe,
    dataFrom: Instant,
    dataTo: Instant,
    replayFrom: Instant,
    initialSymbol: String,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember {
        ReplayChartsPresenter(scope, baseTimeframe, dataFrom, dataTo, replayFrom, initialSymbol, appModule)
    }
    val state by presenter.state.collectAsState()

    ReplayCharts(
        onNewReplay = onNewReplay,
        controlsState = state.controlsState,
        chartState = state.chartState,
        areReplayControlsEnabled = state.areReplayControlsEnabled,
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onSymbolChange = { presenter.event(ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
    )
}

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    controlsState: ReplayControlsState,
    chartState: ReplayChartState,
    areReplayControlsEnabled: Boolean,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    Row(Modifier.fillMaxSize()) {

        ReplayControls(
            state = controlsState,
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
            enabled = areReplayControlsEnabled,
        )

        ResizableChart(
            chart = chartState.chart,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
