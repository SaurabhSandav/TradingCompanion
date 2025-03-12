package com.saurabhsandav.core.ui.landing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

internal data class LandingState(
    val currentScreen: LandingScreen?,
    val openTradesCount: Int?,
    val eventSink: (LandingEvent) -> Unit,
) {

    enum class LandingScreen(
        val title: String,
        val icon: ImageVector,
    ) {
        Account("Account", Icons.Filled.AccountBalance),
        TradeSizing("Trade Sizing", Icons.Filled.Straighten),
        TradeExecutions("Trade Executions", Icons.Filled.FolderOpen),
        Trades("Trades", Icons.Filled.Folder),
        Tags("Tags", Icons.AutoMirrored.Filled.Label),
        Reviews("Reviews", Icons.Filled.RateReview),
        Stats("Stats", Icons.AutoMirrored.Filled.FactCheck),
        ;

        companion object {

            val items = listOf(
                Account,
                TradeSizing,
                TradeExecutions,
                Trades,
                Tags,
                Reviews,
                Stats,
            )
        }
    }
}
