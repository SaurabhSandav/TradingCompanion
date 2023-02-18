package com.saurabhsandav.core.ui.tradeorders.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.tradeorders.orderform.OrderFormWindowParams
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradeOrdersState(
    val tradeOrderItems: ImmutableList<TradeOrderListItem>,
    val orderFormWindowParams: Collection<OrderFormWindowParams>,
) {

    @Immutable
    internal data class TradeOrderEntry(
        val id: Long,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val type: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )

    @Immutable
    internal sealed class TradeOrderListItem {

        @Immutable
        internal data class DayHeader(val header: String) : TradeOrderListItem()

        @Immutable
        internal data class Entries(val entries: ImmutableList<TradeOrderEntry>) : TradeOrderListItem()
    }
}
