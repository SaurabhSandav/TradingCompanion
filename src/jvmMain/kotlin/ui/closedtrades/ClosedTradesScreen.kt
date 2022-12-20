package ui.closedtrades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import ui.addclosedtradedetailed.CloseTradeDetailedWindow
import ui.closedtrades.model.ClosedTradesEvent
import ui.closedtrades.model.ClosedTradesEvent.DeleteConfirmationDialog
import ui.closedtrades.model.ClosedTradesState.FyersLoginWindow
import ui.closedtrades.ui.ClosedTradeChartWindow
import ui.closedtrades.ui.ClosedTradesTable
import ui.closedtrades.ui.DeleteConfirmationDialog
import ui.common.ErrorSnackbar
import ui.common.LocalAppWindowState
import ui.fyerslogin.FyersLoginWindow
import ui.closedtrades.model.ClosedTradesState.DeleteConfirmationDialog as DeleteConfirmationDialogState

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Closed Trades" }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        ClosedTradesTable(
            closedTradesItems = state.closedTradesItems,
            onOpenChart = { presenter.event(ClosedTradesEvent.OpenChart(it)) },
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

        // Chart windows
        state.chartWindowsManager.windows.forEach { windowEntry ->

            key(windowEntry) {

                ClosedTradeChartWindow(
                    onCloseRequest = { windowEntry.close() },
                    chartData = windowEntry.params.chartData,
                )
            }
        }

        // Edit trade windows
        state.editTradeWindowStates.forEach { windowState ->

            key(windowState) {

                CloseTradeDetailedWindow(windowState)
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
