package com.saurabhsandav.core.ui.tradeexecutions.model

import com.saurabhsandav.core.ui.tradeexecutions.model.TradeOrdersState.ProfileOrderId

internal sealed class TradeOrdersEvent {

    data object NewOrder : TradeOrdersEvent()

    data class NewOrderFromExisting(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class EditOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class LockOrders(val ids: List<ProfileOrderId>) : TradeOrdersEvent()

    data class DeleteOrders(val ids: List<ProfileOrderId>) : TradeOrdersEvent()
}
