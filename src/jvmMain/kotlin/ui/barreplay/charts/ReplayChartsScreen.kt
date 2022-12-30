package ui.barreplay.charts

import LocalAppModule
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import kotlinx.datetime.Instant
import trading.Timeframe
import ui.barreplay.charts.model.ReplayChartInfo
import ui.barreplay.charts.model.ReplayChartTabsState
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayChartPage
import ui.barreplay.charts.ui.ReplayControls
import ui.common.chart.state.ChartPageState

@Composable
internal fun ReplayChartsScreen(
    onNewReplay: () -> Unit,
    baseTimeframe: Timeframe,
    candlesBefore: Int,
    replayFrom: Instant,
    dataTo: Instant,
    replayFullBar: Boolean,
    initialSymbol: String,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        ReplayChartsPresenter(
            coroutineScope = scope,
            baseTimeframe = baseTimeframe,
            candlesBefore = candlesBefore,
            replayFrom = replayFrom,
            dataTo = dataTo,
            replayFullBar = replayFullBar,
            initialSymbol = initialSymbol,
            appModule = appModule
        )
    }
    val state by presenter.state.collectAsState()

    ReplayCharts(
        onNewReplay = onNewReplay,
        chartTabsState = state.chartTabsState,
        chartPageState = state.chartPageState,
        chartInfo = state.chartInfo,
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
        onNewChart = { presenter.event(NewChart) },
        onMoveTabBackward = { presenter.event(MoveTabBackward) },
        onMoveTabForward = { presenter.event(MoveTabForward) },
        onCloseChart = { presenter.event(CloseChart(it)) },
        onSelectChart = { presenter.event(SelectChart(it)) },
        onPreviousTab = { presenter.event(PreviousChart) },
        onNextTab = { presenter.event(NextChart) },
        onSymbolChange = { presenter.event(ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
    )
}

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    chartTabsState: ReplayChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onNewChart: () -> Unit,
    onMoveTabBackward: () -> Unit,
    onMoveTabForward: () -> Unit,
    onSelectChart: (Int) -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onCloseChart: (Int) -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Row(Modifier.fillMaxSize().onPreviewKeyEvent { keyEvent ->

        when {
            keyEvent.isCtrlPressed &&
                    keyEvent.key == Key.Tab &&
                    keyEvent.type == KeyEventType.KeyDown -> {

                when {
                    keyEvent.isShiftPressed -> onPreviousTab()
                    else -> onNextTab()
                }

                true
            }

            else -> false
        }
    }) {

        ReplayControls(
            chartInfo = chartInfo,
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
            onNewChart = onNewChart,
            onMoveTabBackward = onMoveTabBackward,
            onMoveTabForward = onMoveTabForward,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
        )

        ReplayChartPage(
            chartTabsState = chartTabsState,
            chartPageState = chartPageState,
            onSelectChart = onSelectChart,
            onCloseChart = onCloseChart,
        )
    }
}
