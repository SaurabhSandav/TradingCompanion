package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeorderform.OrderFormWindow
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.*
import com.saurabhsandav.core.ui.tradeorders.ui.TradeOrdersSelectionBar
import com.saurabhsandav.core.ui.tradeorders.ui.TradeOrdersTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeOrdersScreen(
    onNewOrder: () -> Unit,
    tradeOrderItems: ImmutableList<TradeOrderListItem>,
    onNewOrderFromExisting: (ProfileOrderId) -> Unit,
    onEditOrder: (ProfileOrderId) -> Unit,
    onLockOrders: (List<ProfileOrderId>) -> Unit,
    onDeleteOrders: (List<ProfileOrderId>) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trade Orders")

    val snackbarHostState = remember { SnackbarHostState() }

    val selectionManager = remember { SelectionManager<TradeOrderEntry>() }

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {

                ExtendedFloatingActionButton(onClick = onNewOrder) {
                    Text(text = "New Order")
                }
            },
        ) {

            TradeOrdersTable(
                tradeOrderItems = tradeOrderItems,
                isMarked = { entry -> entry in selectionManager.selection },
                onClickOrder = { entry -> selectionManager.select(entry) },
                onMarkOrder = { entry -> selectionManager.multiSelect(entry) },
                onNewOrder = onNewOrderFromExisting,
                onEditOrder = onEditOrder,
                onLockOrder = { profileOrderId -> onLockOrders(listOf(profileOrderId)) },
                onDeleteOrder = { profileOrderId -> onDeleteOrders(listOf(profileOrderId)) },
            )

            // Errors
            errors.forEach { errorMessage ->

                ErrorSnackbar(snackbarHostState, errorMessage)
            }
        }

        TradeOrdersSelectionBar(
            selectionManager = selectionManager,
            onLockOrders = onLockOrders,
            onDeleteOrders = onDeleteOrders,
        )
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
