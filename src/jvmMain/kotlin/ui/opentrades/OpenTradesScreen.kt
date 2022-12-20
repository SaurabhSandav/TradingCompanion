package ui.opentrades

import androidx.compose.runtime.*
import ui.addclosedtrade.CloseTradeWindow
import ui.addopentrade.AddOpenTradeWindow
import ui.common.LocalAppWindowState
import ui.opentrades.model.OpenTradesEvent
import ui.opentrades.model.OpenTradesEvent.DeleteTrade
import ui.opentrades.ui.OpenTradesTable

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
        onDeleteTrade = { presenter.event(DeleteTrade(it)) },
        onAddTrade = { presenter.event(OpenTradesEvent.AddTrade) },
        onCloseTrade = { presenter.event(OpenTradesEvent.CloseTrade(it)) },
    )

    // Add Trade windows
    state.addTradeWindowStates.forEach { windowState ->

        key(windowState) {
            AddOpenTradeWindow(windowState)
        }
    }

    // Close Trade windows
    state.closeTradeWindowStates.forEach { windowState ->

        key(windowState) {
            CloseTradeWindow(windowState)
        }
    }
}
