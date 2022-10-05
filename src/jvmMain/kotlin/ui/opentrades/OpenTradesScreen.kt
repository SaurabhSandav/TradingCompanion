package ui.opentrades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ui.opentrades.model.AddTradeWindowState
import ui.opentrades.model.OpenTradesEvent.AddTradeWindow
import ui.opentrades.model.OpenTradesEvent.DeleteTrade
import ui.opentrades.ui.AddOpenTradeWindow
import ui.opentrades.ui.OpenTradesTable

@Composable
internal fun OpenTradesScreen(
    presenter: OpenTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    OpenTradesTable(
        openTrades = state.openTrades,
        onEditTrade = { presenter.event(AddTradeWindow.OpenEdit(it)) },
        onDeleteTrade = { presenter.event(DeleteTrade(it)) },
        onAddTrade = { presenter.event(AddTradeWindow.Open) },
    )

    val addOpenTradeWindowState = state.addTradeWindowState

    if (addOpenTradeWindowState is AddTradeWindowState.Open) {

        AddOpenTradeWindow(
            onCloseRequest = { presenter.event(AddTradeWindow.Close) },
            addOpenTradeFormStateModel = addOpenTradeWindowState.formState,
            onSaveTrade = { presenter.event(AddTradeWindow.SaveTrade(it)) },
        )
    }
}
