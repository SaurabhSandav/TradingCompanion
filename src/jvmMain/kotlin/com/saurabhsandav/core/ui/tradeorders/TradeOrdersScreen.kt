package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindow
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.*
import com.saurabhsandav.core.ui.tradeorders.ui.TradeOrdersTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeOrdersScreen(
    onNewOrder: () -> Unit,
    tradeOrderItems: ImmutableList<TradeOrderListItem>,
    onNewOrderFromExisting: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrder: (ProfileOrderId) -> Unit,
    onDeleteOrder: (ProfileOrderId) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trade Orders")

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {

            ExtendedFloatingActionButton(onClick = onNewOrder) {
                Text(text = "New Order")
            }
        },
    ) {

        TradeOrdersTable(
            tradeOrderItems = tradeOrderItems,
            onNewOrder = onNewOrderFromExisting,
            onEditOrder = onEditOrder,
            onLockOrder = onLockOrder,
            onDeleteOrder = onDeleteOrder,
        )

        // Errors
        errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}

@Composable
internal fun TradeOrdersScreenWindows(
    orderFormWindowsManager: AppWindowsManager<OrderFormParams>,
) {

    // Order form windows
    orderFormWindowsManager.Windows { window ->

        OrderFormWindow(
            profileId = window.params.profileId,
            formType = window.params.formType,
            onCloseRequest = window::close,
        )
    }
}
