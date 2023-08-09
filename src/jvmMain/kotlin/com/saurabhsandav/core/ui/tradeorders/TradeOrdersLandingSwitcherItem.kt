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
            onNewOrder = { presenter.event(NewOrder) },
            tradeOrderItems = state.tradeOrderItems,
            onNewOrderFromExisting = { presenter.event(NewOrderFromExisting(it)) },
            onLockOrder = { presenter.event(LockOrder(it)) },
            onEditOrder = { presenter.event(EditOrder(it)) },
            onDeleteOrder = { presenter.event(DeleteOrder(it)) },
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
