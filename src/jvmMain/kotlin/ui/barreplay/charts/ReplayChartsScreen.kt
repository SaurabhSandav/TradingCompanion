package ui.barreplay.charts

import AppModule
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
import ui.common.chart.state.ChartState

@Composable
internal fun ReplayChartsScreen(
    appModule: AppModule,
    onNewReplay: () -> Unit,
    baseTimeframe: Timeframe,
    candlesBefore: Int,
    replayFrom: Instant,
    dataTo: Instant,
    replayFullBar: Boolean,
    initialSymbol: String,
) {

    val scope = rememberCoroutineScope()
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
        chartState = state.chartState,
        chartInfo = state.chartInfo,
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
        onNewChart = { presenter.event(NewChart) },
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
    chartState: ChartState,
    chartInfo: ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onNewChart: () -> Unit,
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
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
        )

        ReplayChartPage(
            chartTabsState = chartTabsState,
            chartState = chartState,
            onSelectChart = onSelectChart,
            onCloseChart = onCloseChart,
        )
    }
}
