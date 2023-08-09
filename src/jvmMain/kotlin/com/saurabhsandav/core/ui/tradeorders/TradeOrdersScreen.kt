package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindow
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import com.saurabhsandav.core.ui.tradeorders.ui.TradeOrdersTable

@Composable
internal fun TradeOrdersScreen(
    presenter: TradeOrdersPresenter,
) {

    val state by presenter.state.collectAsState()

    // Set window title
    WindowTitle("Trade Orders")

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
            onLockOrder = { presenter.event(LockOrder(it)) },
            onEditOrder = { presenter.event(EditOrder(it)) },
            onDeleteOrder = { presenter.event(DeleteOrder(it)) },
        )

        // Order form windows
        state.orderFormWindowsManager.Windows { window ->

            OrderFormWindow(
                profileId = window.params.profileId,
                formType = window.params.formType,
                onCloseRequest = window::close,
            )
        }

        // Errors
        presenter.errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
