package com.saurabhsandav.core.ui.tradeorders.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.tradeorders.orderform.OrderFormWindowParams

@Immutable
internal data class TradeOrdersState(
    val tradeOrderItems: Map<TradeOrderListItem.DayHeader, List<TradeOrderListItem.Entry>>,
    val orderFormWindowParams: Collection<OrderFormWindowParams>,
    val deleteConfirmationDialogState: DeleteConfirmationDialog,
) {

    @Immutable
    internal sealed class DeleteConfirmationDialog {

        @Immutable
        data class Open(val id: Long) : DeleteConfirmationDialog()

        @Immutable
        object Dismissed : DeleteConfirmationDialog()
    }
}

@Immutable
internal sealed class TradeOrderListItem {

    @Immutable
    internal data class DayHeader(val header: String) : TradeOrderListItem()

    @Immutable
    internal data class Entry(
        val id: Long,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val type: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    ) : TradeOrderListItem()
}
