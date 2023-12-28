package com.saurabhsandav.core.ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.ui.TradesTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradesScreen(
    openTrades: ImmutableList<TradeEntry>,
    todayTrades: ImmutableList<TradeEntry>,
    pastTrades: ImmutableList<TradeEntry>,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trades")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        TradesTable(
            openTrades = openTrades,
            todayTrades = todayTrades,
            pastTrades = pastTrades,
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
        )

        // Errors
        errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
