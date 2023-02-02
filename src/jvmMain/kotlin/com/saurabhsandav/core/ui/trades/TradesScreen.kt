package com.saurabhsandav.core.ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesState.FyersLoginWindow
import com.saurabhsandav.core.ui.trades.ui.TradeChartWindow
import com.saurabhsandav.core.ui.trades.ui.TradesTable

@Composable
internal fun TradesScreen(
    presenter: TradesPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Trades" }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        TradesTable(
            tradesItems = state.tradesItems,
            onOpenChart = { presenter.event(OpenChart(it)) },
        )

        // Chart windows
        state.chartWindowsManager.windows.forEach { windowEntry ->

            key(windowEntry) {

                TradeChartWindow(
                    onCloseRequest = windowEntry::close,
                    chartData = windowEntry.params.chartData,
                )
            }
        }

        // Fyers login window
        val fyersLoginWindowState = state.fyersLoginWindowState

        if (fyersLoginWindowState is FyersLoginWindow.Open) {

            FyersLoginWindow(fyersLoginWindowState.fyersLoginState)
        }

        // Errors
        presenter.errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
