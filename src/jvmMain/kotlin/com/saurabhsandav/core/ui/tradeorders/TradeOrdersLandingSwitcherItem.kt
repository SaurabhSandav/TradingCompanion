package com.saurabhsandav.core.ui.tradeorders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersEvent.*
import kotlinx.coroutines.CoroutineScope

internal class TradeOrdersLandingSwitcherItem(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
) : LandingSwitcherItem {

    private val presenter = TradeOrdersPresenter(coroutineScope, appModule)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradeOrdersScreen(
            onNewOrder = { state.eventSink(NewOrder) },
            tradeOrderItems = state.tradeOrderItems,
            onNewOrderFromExisting = { state.eventSink(NewOrderFromExisting(it)) },
            onLockOrders = { ids -> state.eventSink(LockOrders(ids)) },
            onEditOrder = { state.eventSink(EditOrder(it)) },
            onDeleteOrders = { ids -> state.eventSink(DeleteOrders(ids)) },
            errors = state.errors,
        )
    }

    @Composable
    override fun Windows() {

        val state by presenter.state.collectAsState()

        TradeOrdersScreenWindows(
            orderFormWindowsManager = state.orderFormWindowsManager,
        )
    }
}
