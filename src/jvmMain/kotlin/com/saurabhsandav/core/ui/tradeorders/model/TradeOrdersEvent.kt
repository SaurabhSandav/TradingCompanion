package com.saurabhsandav.core.ui.tradeorders.model

import java.util.*

internal sealed class TradeOrdersEvent {

    object NewOrder : TradeOrdersEvent()

    data class NewOrderFromExisting(val id: Long) : TradeOrdersEvent()

    data class EditOrder(val id: Long) : TradeOrdersEvent()

    data class CloseOrderForm(val id: UUID) : TradeOrdersEvent()

    data class LockOrder(val id: Long) : TradeOrdersEvent()

    data class DeleteOrder(val id: Long) : TradeOrdersEvent()
}
