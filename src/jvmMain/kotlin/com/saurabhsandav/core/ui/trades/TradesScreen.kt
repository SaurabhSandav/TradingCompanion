package com.saurabhsandav.core.ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.trades.detail.TradeDetailWindow
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
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
        state.tradeDetailWindowsManager.Windows { window ->

            TradeDetailWindow(
                profileId = window.params.profileId,
                tradeId = window.params.tradeId,
                onCloseRequest = window::close,
            )
        }

        // Chart windows
        state.chartWindowsManager.Windows { window ->

            TradeChartWindow(
                onCloseRequest = window::close,
                chartData = window.params.chartData,
            )
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
