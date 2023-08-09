package com.saurabhsandav.core.ui.tradeorders.model

import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.ProfileOrderId

internal sealed class TradeOrdersEvent {

    data object NewOrder : TradeOrdersEvent()

    data class NewOrderFromExisting(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class EditOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class LockOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class DeleteOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()
}
