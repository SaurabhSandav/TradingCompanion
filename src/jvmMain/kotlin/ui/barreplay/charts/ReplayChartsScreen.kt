package ui.barreplay.charts

import AppModule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import trading.Timeframe
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartTabsState
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayChartSwitcher
import ui.barreplay.charts.ui.ReplayControls

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
        chartTabsState = state.chartTabsState,
        chartState = state.chartState,
        onReset = { presenter.event(Reset) },
        onNext = { presenter.event(Next) },
        onIsAutoNextEnabledChange = { presenter.event(ChangeIsAutoNextEnabled(it)) },
        onNewChart = { presenter.event(NewChart) },
        onCloseChart = { presenter.event(CloseChart(it)) },
        onSelectChart = { presenter.event(SelectChart(it)) },
        onSymbolChange = { presenter.event(ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
    )
}

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    chartTabsState: ReplayChartTabsState,
    chartState: ReplayChartState?,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onNewChart: () -> Unit,
    onSelectChart: (Int) -> Unit,
    onCloseChart: (Int) -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Row(Modifier.fillMaxSize()) {

        if (chartState == null) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = { CircularProgressIndicator() },
            )
        } else {

            ReplayControls(
                chartState = chartState,
                onNewReplay = onNewReplay,
                onReset = onReset,
                onNext = onNext,
                onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
                onNewChart = onNewChart,
                onSymbolChange = onSymbolChange,
                onTimeframeChange = onTimeframeChange,
            )

            ReplayChartSwitcher(
                chartTabsState = chartTabsState,
                chartState = chartState,
                onSelectChart = onSelectChart,
                onCloseChart = onCloseChart,
            )
        }
    }
}
