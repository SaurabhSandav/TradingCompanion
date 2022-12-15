package ui.charts

import AppModule
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import ui.charts.model.ChartsEvent.*
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.FyersLoginWindow
import ui.charts.ui.ChartControls
import ui.charts.ui.ChartTabRow
import ui.common.AppWindow
import ui.common.ErrorSnackbar
import ui.common.UIErrorMessage
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartState
import ui.fyerslogin.FyersLoginWindow

@Composable
internal fun ChartsWindow(
    appModule: AppModule,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { ChartsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        onCloseRequest = onCloseRequest,
    ) {

        ChartsScreen(
            tabsState = state.tabsState,
            chartState = state.chartState,
            chartInfo = state.chartInfo,
            onNewChart = { presenter.event(NewChart) },
            onCloseChart = { presenter.event(CloseChart(it)) },
            onSelectChart = { presenter.event(SelectChart(it)) },
            onSymbolChange = { presenter.event(ChangeSymbol(it)) },
            onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
            fyersLoginWindowState = state.fyersLoginWindowState,
            errors = state.errors,
        )
    }
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
    fyersLoginWindowState: FyersLoginWindow,
    errors: List<UIErrorMessage>,
) {

    Row(Modifier.fillMaxSize()) {

        ChartControls(
            chartInfo = chartInfo,
            onNewChart = onNewChart,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
        )

        Column {

            ChartTabRow(
                tabsState = tabsState,
                onSelectChart = onSelectChart,
                onCloseChart = onCloseChart,
            )

            Column(Modifier.weight(1F)) {

                ChartPage(
                    state = chartState,
                    modifier = Modifier.weight(1F),
                )

                val snackbarHostState = remember { SnackbarHostState() }

                // Errors
                errors.forEach { errorMessage ->

                    ErrorSnackbar(snackbarHostState, errorMessage)
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.animateContentSize().align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    // Fyers login window
    if (fyersLoginWindowState is FyersLoginWindow.Open) {

        FyersLoginWindow(fyersLoginWindowState.fyersLoginState)
    }
}
