package com.saurabhsandav.core.ui.tradeorders.model

import com.saurabhsandav.core.ui.tradeorders.model.TradeOrdersState.ProfileOrderId
import java.util.*

internal sealed class TradeOrdersEvent {

    object NewOrder : TradeOrdersEvent()

    data class NewOrderFromExisting(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class EditOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class CloseOrderForm(val id: UUID) : TradeOrdersEvent()

    data class LockOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()

    data class DeleteOrder(val profileOrderId: ProfileOrderId) : TradeOrdersEvent()
}
