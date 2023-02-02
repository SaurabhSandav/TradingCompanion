package com.saurabhsandav.core.ui.opentrades

import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.closetradeform.CloseTradeFormWindow
import com.saurabhsandav.core.ui.closetradeform.rememberCloseTradeFormWindowState
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.opentradeform.OpenTradeFormWindow
import com.saurabhsandav.core.ui.opentradeform.rememberOpenTradeFormWindowState
import com.saurabhsandav.core.ui.opentrades.model.OpenTradesEvent
import com.saurabhsandav.core.ui.opentrades.model.OpenTradesEvent.DeleteTrade
import com.saurabhsandav.core.ui.opentrades.ui.OpenTradesTable
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.pnlcalculator.rememberPNLCalculatorWindowState

@Composable
internal fun OpenTradesScreen(
    presenter: OpenTradesPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Open Trades" }

    OpenTradesTable(
        openTrades = state.openTrades,
        onEditTrade = { presenter.event(OpenTradesEvent.EditTrade(it)) },
        onOpenPNLCalculator = { presenter.event(OpenTradesEvent.OpenPNLCalculator(it)) },
        onDeleteTrade = { presenter.event(DeleteTrade(it)) },
        onAddTrade = { presenter.event(OpenTradesEvent.AddTrade) },
        onCloseTrade = { presenter.event(OpenTradesEvent.CloseTrade(it)) },
    )

    // Add Trade windows
    state.openTradeFormWindowParams.forEach { params ->

        key(params) {

            OpenTradeFormWindow(rememberOpenTradeFormWindowState(params))
        }
    }

    // PNL Calculator windows
    state.pnlCalculatorWindowParams.forEach { params ->

        key(params) {

            PNLCalculatorWindow(rememberPNLCalculatorWindowState(params))
        }
    }

    // Close Trade windows
    state.closeTradeFormWindowParams.forEach { params ->

        key(params) {

            CloseTradeFormWindow(rememberCloseTradeFormWindowState(params))
        }
    }
}
