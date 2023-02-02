package com.saurabhsandav.core.ui.tradeorders.model

internal sealed class TradeOrdersEvent {

    object NewOrder : TradeOrdersEvent()

    data class NewOrderFromExisting(val id: Long) : TradeOrdersEvent()

    data class EditOrder(val id: Long) : TradeOrdersEvent()

    data class DeleteOrder(val id: Long) : TradeOrdersEvent()

    sealed class DeleteConfirmationDialog : TradeOrdersEvent() {

        data class Confirm(val id: Long) : DeleteConfirmationDialog()

        object Dismiss : DeleteConfirmationDialog()
    }
}
