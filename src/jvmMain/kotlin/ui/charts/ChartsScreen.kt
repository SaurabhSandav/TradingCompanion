package ui.charts

import AppModule
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ui.charts.model.ChartsEvent.*
import ui.charts.model.ChartsState
import ui.charts.ui.ChartControls
import ui.charts.ui.ChartPage
import ui.common.chart.state.ChartState

@Composable
internal fun ChartsScreen(appModule: AppModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { ChartsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    ChartsScreen(
        tabsState = state.tabsState,
        chartState = state.chartState,
        chartInfo = state.chartInfo,
        onNewChart = { presenter.event(NewChart) },
        onCloseChart = { presenter.event(CloseChart(it)) },
        onSelectChart = { presenter.event(SelectChart(it)) },
        onSymbolChange = { presenter.event(ChangeSymbol(it)) },
        onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
    )
}

@Composable
private fun ChartsScreen(
    tabsState: ChartsState.TabsState,
    chartState: ChartState,
    chartInfo: ChartsState.ChartInfo,
    onNewChart: () -> Unit,
    onSelectChart: (Int) -> Unit,
    onCloseChart: (Int) -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Row(Modifier.fillMaxSize()) {

        ChartControls(
            chartInfo = chartInfo,
            onNewChart = onNewChart,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
        )

        ChartPage(
            tabsState = tabsState,
            chartState = chartState,
            onSelectChart = onSelectChart,
            onCloseChart = onCloseChart,
        )
    }
}
