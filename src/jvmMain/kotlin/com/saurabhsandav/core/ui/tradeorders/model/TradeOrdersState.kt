package com.saurabhsandav.core.ui.tradeorders.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradeOrdersState(
    val tradeOrderItems: ImmutableList<TradeOrderListItem>,
    val selectionManager: SelectionManager<TradeOrderEntry>,
    val errors: ImmutableList<UIErrorMessage>,
    val eventSink: (TradeOrdersEvent) -> Unit,
) {

    @Immutable
    internal data class TradeOrderEntry(
        val profileOrderId: ProfileOrderId,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
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

    @Immutable
    data class ProfileOrderId(
        val profileId: Long,
        val orderId: Long,
    )
}
