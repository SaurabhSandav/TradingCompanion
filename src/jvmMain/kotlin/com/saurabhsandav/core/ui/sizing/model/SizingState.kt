package com.saurabhsandav.core.ui.sizing.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import kotlinx.collections.immutable.ImmutableList
import java.util.*

@Immutable
internal data class SizingState(
    val sizedTrades: ImmutableList<SizedTrade>,
    val orderFormWindowsManager: AppWindowsManager<OrderFormParams>,
    val eventSink: (SizingEvent) -> Unit,
) {

    @Immutable
    internal data class SizedTrade(
        val id: Long,
        val ticker: String,
        val entry: String,
        val stop: String,
        val side: String,
        val spread: String,
        val calculatedQuantity: String,
        val maxAffordableQuantity: String,
        val target: String,
        val color: Color,
    )

    @Immutable
    internal data class OrderFormParams(
        val id: UUID,
        val profileId: Long,
        val formType: OrderFormType,
        val onOrderSaved: (orderId: Long) -> Unit,
    )
}
