package ui.closedtrades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ui.addclosedtrade.CloseTradeWindow
import ui.closedtrades.model.ClosedTradesEvent
import ui.closedtrades.model.ClosedTradesEvent.EditTradeWindow
import ui.closedtrades.model.EditTradeWindowState
import ui.closedtrades.ui.ClosedTradesTable

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    ClosedTradesTable(
        closedTradesItems = state.closedTradesItems,
        onEditTrade = { presenter.event(EditTradeWindow.Open(it)) },
        onDeleteTrade = { presenter.event(ClosedTradesEvent.DeleteTrade(it)) },
    )

    val closeTradeWindowState = state.closeTradeWindowState

    if (closeTradeWindowState is EditTradeWindowState.Open) {

        CloseTradeWindow(
            onCloseRequest = { presenter.event(EditTradeWindow.Close) },
            formModel = closeTradeWindowState.formModel,
            onSaveTrade = { presenter.event(EditTradeWindow.SaveTrade(it)) },
        )
    }
}
