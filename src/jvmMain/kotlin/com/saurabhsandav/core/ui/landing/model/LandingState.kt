package com.saurabhsandav.core.ui.landing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class LandingState(
    val currentScreen: LandingScreen?,
    val openTradesCount: Long?,
    val eventSink: (LandingEvent) -> Unit,
) {

    @Immutable
    enum class LandingScreen(
        val title: String,
        val icon: ImageVector,
    ) {
        Account("Account", Icons.Filled.AccountBalance),
        TradeSizing("Trade Sizing", Icons.Filled.Straighten),
        TradeExecutions("Trade Executions", Icons.Filled.FolderOpen),
        Trades("Trades", Icons.Filled.Folder),
        Studies("Studies", Icons.Filled.FactCheck);

        companion object {

            val items = listOf(
                Account,
                TradeSizing,
                TradeExecutions,
                Trades,
                Studies,
            )
        }
    }
}
