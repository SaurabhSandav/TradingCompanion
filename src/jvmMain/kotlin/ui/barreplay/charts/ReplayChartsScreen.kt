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
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayChartPage
import ui.barreplay.charts.ui.ReplayControls
import ui.common.chart.state.ChartPageState
import ui.stockchart.StockChartTabsState

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
        tabsState = state.tabsState,
        chartPageState = state.chartPageState,
        chartInfo = state.chartInfo,
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
        onSymbolChange = { presenter.event(ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
    )
}

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Row(Modifier.fillMaxSize().onPreviewKeyEvent { keyEvent ->

        when {
            keyEvent.isCtrlPressed &&
                    keyEvent.key == Key.Tab &&
                    keyEvent.type == KeyEventType.KeyDown -> {

                when {
                    keyEvent.isShiftPressed -> tabsState.selectPreviousTab()
                    else -> tabsState.selectNextTab()
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
            tabsState = tabsState,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
        )

        ReplayChartPage(
            tabsState = tabsState,
            chartPageState = chartPageState,
        )
    }
}
