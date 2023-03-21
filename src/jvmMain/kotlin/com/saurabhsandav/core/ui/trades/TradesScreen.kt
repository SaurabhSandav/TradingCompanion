package com.saurabhsandav.core.ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.trades.detail.TradeDetailWindow
import com.saurabhsandav.core.ui.trades.model.TradesEvent.*
import com.saurabhsandav.core.ui.trades.model.TradesState.FyersLoginWindow
import com.saurabhsandav.core.ui.trades.ui.TradeChartWindow
import com.saurabhsandav.core.ui.trades.ui.TradesTable

@Composable
internal fun TradesScreen(
    presenter: TradesPresenter,
) {

    val state by presenter.state.collectAsState()

    // Set window title
    WindowTitle("Trades")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        TradesTable(
            tradesItems = state.tradesItems,
            onOpenDetails = { presenter.event(OpenDetails(it)) },
            onOpenChart = { presenter.event(OpenChart(it)) },
        )

        // Detail Windows
        state.showTradeDetailIds.forEach { tradeId ->

            key(tradeId) {

                val windowOwner = remember { AppWindowOwner() }

                LaunchedEffect(state.bringDetailsToFrontId) {

                    if (state.bringDetailsToFrontId == tradeId) {
                        windowOwner.childrenToFront()
                        presenter.event(DetailsBroughtToFront)
                    }
                }

                AppWindowOwner(windowOwner) {

                    TradeDetailWindow(
                        tradeId = tradeId,
                        onCloseRequest = { presenter.event(CloseDetails(tradeId)) },
                    )
                }
            }
        }

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
