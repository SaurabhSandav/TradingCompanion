package ui.charts

import AppModule
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import ui.charts.model.ChartsEvent.*
import ui.charts.ui.ChartsScreen
import ui.common.AppWindow

@Composable
internal fun ChartsWindow(
    appModule: AppModule,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { ChartsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    ChartsWindow(
        onPreviousTab = { presenter.event(PreviousChart) },
        onNextTab = { presenter.event(NextChart) },
        onCloseRequest = onCloseRequest,
    ) {

        ChartsScreen(
            tabsState = state.tabsState,
            chartState = state.chartState,
            chartInfo = state.chartInfo,
            onNewChart = { presenter.event(NewChart) },
            onMoveTabBackward = { presenter.event(MoveTabBackward) },
            onMoveTabForward = { presenter.event(MoveTabForward) },
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
private fun ChartsWindow(
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
        onCloseRequest = onCloseRequest,
        onPreviewKeyEvent = { keyEvent ->

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
        },
        content = { content() },
    )
}
