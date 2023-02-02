package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import com.saurabhsandav.core.ui.tradeorders.orderform.OrderFormWindow
import com.saurabhsandav.core.ui.tradeorders.orderform.rememberOrderFormWindowState
import com.saurabhsandav.core.ui.tradeorders.ui.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.tradeorders.ui.TradeOrdersTable
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.DeleteConfirmationDialog as DeleteConfirmationDialogState

@Composable
internal fun TradeOrdersScreen(
    presenter: TradeOrdersPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Closed Trades" }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { presenter.event(NewOrder) }) {
                Text(text = "New Order")
            }
        },
    ) {

        TradeOrdersTable(
            tradeOrderItems = state.tradeOrderItems,
            onNewOrder = { presenter.event(NewOrderFromExisting(it)) },
            onEditOrder = { presenter.event(EditOrder(it)) },
            onDeleteOrder = { presenter.event(DeleteOrder(it)) },
        )

        // New Order windows
        state.orderFormWindowParams.forEach { params ->

            key(params) {

                OrderFormWindow(rememberOrderFormWindowState(params))
            }
        }

        // Delete order confirmation dialog
        val deleteConfirmationDialogState = state.deleteConfirmationDialogState

        if (deleteConfirmationDialogState is DeleteConfirmationDialogState.Open) {

            DeleteConfirmationDialog(
                onDismiss = { presenter.event(DeleteConfirmationDialog.Dismiss) },
                onConfirm = { presenter.event(DeleteConfirmationDialog.Confirm(deleteConfirmationDialogState.id)) },
            )
        }

        // Errors
        presenter.errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
