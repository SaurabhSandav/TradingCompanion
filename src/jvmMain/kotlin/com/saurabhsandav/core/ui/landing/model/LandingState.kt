package com.saurabhsandav.core.ui.landing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class LandingState(
    val currentScreen: LandingScreen? = null,
)

enum class LandingScreen(
    val title: String,
    val icon: ImageVector,
) {
    Account("Account", Icons.Filled.AccountBalance),
    TradeSizing("Trade Sizing", Icons.Filled.Straighten),
    OpenTrades("Open Trades", Icons.Filled.FolderOpen),
    ClosedTrades("Closed Trades", Icons.Filled.Folder),
    TradeOrders("Orders", Icons.Filled.FolderOpen),
    Trades("Trades", Icons.Filled.Folder),
    Studies("Studies", Icons.Filled.FactCheck);

    companion object {

        val items = listOf(
            Account,
            TradeSizing,
            OpenTrades,
            ClosedTrades,
            TradeOrders,
            Trades,
            Studies,
        )
    }
}
