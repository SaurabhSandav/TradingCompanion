package ui.charts

import LocalAppModule
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import ui.charts.model.ChartsEvent.ChangeSymbol
import ui.charts.model.ChartsEvent.ChangeTimeframe
import ui.charts.ui.ChartsScreen
import ui.common.app.AppWindow

@Composable
internal fun ChartsWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { ChartsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        title = "Charts",
        onCloseRequest = onCloseRequest,
    ) {

        ChartsScreen(
            tabsState = state.tabsState,
            chartPageState = state.chartPageState,
            chartInfo = state.chartInfo,
            onSymbolChange = { presenter.event(ChangeSymbol(it)) },
            onTimeframeChange = { presenter.event(ChangeTimeframe(it)) },
            fyersLoginWindowState = state.fyersLoginWindowState,
            errors = state.errors,
        )
    }
}
