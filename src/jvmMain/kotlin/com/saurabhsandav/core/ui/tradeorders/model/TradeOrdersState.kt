package com.saurabhsandav.core.ui.tradeorders.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import kotlinx.collections.immutable.ImmutableList
import java.util.*

@Immutable
internal data class TradeOrdersState(
    val tradeOrderItems: ImmutableList<TradeOrderListItem>,
    val orderFormWindowsManager: AppWindowsManager<OrderFormParams>,
    val errors: ImmutableList<UIErrorMessage>,
) {

    @Immutable
    internal data class TradeOrderEntry(
        val profileOrderId: ProfileOrderId,
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

    @Immutable
    internal data class OrderFormParams(
        val id: UUID,
        val profileId: Long,
        val formType: OrderFormType,
    )

    @Immutable
    data class ProfileOrderId(
        val profileId: Long,
        val orderId: Long,
    )
}
