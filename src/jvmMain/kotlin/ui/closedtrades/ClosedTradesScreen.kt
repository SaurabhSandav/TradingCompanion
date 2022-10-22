package ui.closedtrades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import ui.addclosedtradedetailed.CloseTradeDetailedWindow
import ui.candledownload.ui.FyersLoginWebView
import ui.closedtrades.model.ClosedTradesEvent
import ui.closedtrades.model.ClosedTradesEvent.DeleteConfirmationDialog
import ui.closedtrades.model.ClosedTradesState.CandleDataLoginWindow
import ui.closedtrades.ui.ClosedTradeChartWindow
import ui.closedtrades.ui.ClosedTradesTable
import ui.closedtrades.ui.DeleteConfirmationDialog
import ui.common.ErrorSnackbar
import ui.closedtrades.model.ClosedTradesState.DeleteConfirmationDialog as DeleteConfirmationDialogState

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

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
        state.chartWindowsManager.windows.forEach { windowManager ->

            key(windowManager) {

                ClosedTradeChartWindow(
                    onCloseRequest = { windowManager.close() },
                    chartData = windowManager.chartData,
                )
            }
        }

        // Edit trade windows
        state.editTradeWindowsManager.windows.forEach { windowManager ->

            key(windowManager) {

                CloseTradeDetailedWindow(
                    onCloseRequest = { windowManager.close() },
                    formModel = windowManager.formModel,
                    onSaveTrade = { presenter.event(ClosedTradesEvent.SaveTrade(it)) },
                )
            }
        }

        // Edit trade windows
        val candleDataLoginWindowState = state.candleDataLoginWindowState

        if (candleDataLoginWindowState is CandleDataLoginWindow.Open) {

            Window(
                onCloseRequest = { presenter.event(ClosedTradesEvent.DismissCandleDataWindow) }
            ) {

                FyersLoginWebView(candleDataLoginWindowState.loginUrl) {
                    presenter.event(ClosedTradesEvent.CandleDataLoggedIn(it))
                }
            }
        }

        // Errors
        presenter.errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
