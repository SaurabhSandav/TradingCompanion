package ui.closedtrades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import ui.addclosedtradedetailed.CloseTradeDetailedWindow
import ui.closedtrades.model.ClosedTradesEvent
import ui.closedtrades.model.ClosedTradesEvent.DeleteConfirmationDialog
import ui.closedtrades.ui.ClosedTradesTable
import ui.closedtrades.ui.DeleteConfirmationDialog
import ui.closedtrades.model.ClosedTradesState.DeleteConfirmationDialog as DeleteConfirmationDialogState

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    ClosedTradesTable(
        closedTradesItems = state.closedTradesItems,
        onEditTrade = { presenter.event(ClosedTradesEvent.EditTrade(it)) },
        onDeleteTrade = { presenter.event(ClosedTradesEvent.DeleteTrade(it)) },
    )

    val deleteConfirmationDialogState = state.deleteConfirmationDialogState

    if (deleteConfirmationDialogState is DeleteConfirmationDialogState.Open) {

        DeleteConfirmationDialog(
            onDismiss = { presenter.event(DeleteConfirmationDialog.Dismiss) },
            onConfirm = { presenter.event(DeleteConfirmationDialog.Confirm(deleteConfirmationDialogState.id)) },
        )
    }

    state.editTradeWindowsManager.windows.forEach { windowManager ->

        key(windowManager) {

            CloseTradeDetailedWindow(
                onCloseRequest = { windowManager.close() },
                formModel = windowManager.formModel,
                onSaveTrade = { presenter.event(ClosedTradesEvent.SaveTrade(it)) },
            )
        }
    }
}
