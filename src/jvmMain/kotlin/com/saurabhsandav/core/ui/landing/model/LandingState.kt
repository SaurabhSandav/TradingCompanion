package com.saurabhsandav.core.ui.landing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType

@Immutable
internal data class LandingState(
    val currentScreen: LandingScreen?,
    val eventSink: (LandingEvent) -> Unit,
) {

    enum class LandingScreen(
        val title: String,
        val icon: ImageVector,
    ) {
        Account("Account", Icons.Filled.AccountBalance),
        TradeSizing("Trade Sizing", Icons.Filled.Straighten),
        TradeOrders("Orders", Icons.Filled.FolderOpen),
        Trades("Trades", Icons.Filled.Folder),
        Studies("Studies", Icons.Filled.FactCheck);

        companion object {

            val items = listOf(
                Account,
                TradeSizing,
                TradeOrders,
                Trades,
                Studies,
            )
        }
    }

    @Immutable
    internal data class OrderFormWindowParams(
        val profileId: Long,
        val formType: OrderFormType,
    )

    @Immutable
    internal data class TradeWindowParams(
        val profileId: Long,
        val tradeId: Long,
    )
}
