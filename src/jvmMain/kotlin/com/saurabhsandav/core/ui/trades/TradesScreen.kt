package com.saurabhsandav.core.ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.trade.TradeWindow
import com.saurabhsandav.core.ui.trades.model.TradeChartWindowParams
import com.saurabhsandav.core.ui.trades.model.TradesState.*
import com.saurabhsandav.core.ui.trades.ui.TradeChartWindow
import com.saurabhsandav.core.ui.trades.ui.TradesTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesScreen(
    tradesItems: ImmutableList<TradeListItem>,
    onOpenDetails: (ProfileTradeId) -> Unit,
    onOpenChart: (ProfileTradeId) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trades")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        TradesTable(
            tradesItems = tradesItems,
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
        )

        // Errors
        errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}

@Composable
internal fun TradesScreenWindows(
    tradeWindowsManager: AppWindowsManager<ProfileTradeId>,
    chartWindowsManager: AppWindowsManager<TradeChartWindowParams>,
    fyersLoginWindowState: FyersLoginWindow,
) {

    // Detail Windows
    tradeWindowsManager.Windows { window ->

        TradeWindow(
            profileId = window.params.profileId,
            tradeId = window.params.tradeId,
            onCloseRequest = window::close,
        )
    }

    // Chart windows
    chartWindowsManager.Windows { window ->

        TradeChartWindow(
            onCloseRequest = window::close,
            chartData = window.params.chartData,
        )
    }

    // Fyers login window
    if (fyersLoginWindowState is FyersLoginWindow.Open) {

        FyersLoginWindow(fyersLoginWindowState.fyersLoginState)
    }
}
