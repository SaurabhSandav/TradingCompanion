package ui.barreplay.charts

import LocalAppModule
import androidx.compose.runtime.*
import kotlinx.datetime.Instant
import trading.Timeframe
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayCharts

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
