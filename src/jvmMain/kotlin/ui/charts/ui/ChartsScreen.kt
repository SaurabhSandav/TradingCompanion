package ui.charts.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.FyersLoginWindow
import ui.common.ErrorSnackbar
import ui.common.UIErrorMessage
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartPageState
import ui.fyerslogin.FyersLoginWindow

@Composable
fun ChartsScreen(
    tabsState: ChartsState.TabsState,
    chartPageState: ChartPageState,
    chartInfo: ChartsState.ChartInfo,
    onNewChart: () -> Unit,
    onMoveTabBackward: () -> Unit,
    onMoveTabForward: () -> Unit,
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
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
            onMoveTabBackward = onMoveTabBackward,
            onMoveTabForward = onMoveTabForward,
        )

        Column {

            ChartTabRow(
                tabsState = tabsState,
                onNewChart = onNewChart,
                onSelectChart = onSelectChart,
                onCloseChart = onCloseChart,
            )

            Column(Modifier.weight(1F)) {

                ChartPage(
                    state = chartPageState,
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
