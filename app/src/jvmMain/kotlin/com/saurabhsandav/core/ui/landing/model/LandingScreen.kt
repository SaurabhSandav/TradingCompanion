package com.saurabhsandav.core.ui.landing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

enum class LandingScreen {
    Account,
    TradeSizing,
    TradeExecutions,
    Trades,
    Tags,
    Reviews,
    Stats,
}

fun LandingScreen.getDetails(): LandingScreenDetails = when (this) {
    LandingScreen.Account -> LandingScreenDetails("Account", Icons.Filled.AccountBalance)
    LandingScreen.TradeSizing -> LandingScreenDetails("Trade Sizing", Icons.Filled.Straighten)
    LandingScreen.TradeExecutions -> LandingScreenDetails("Trade Executions", Icons.Filled.FolderOpen)
    LandingScreen.Trades -> LandingScreenDetails("Trades", Icons.Filled.Folder)
    LandingScreen.Tags -> LandingScreenDetails("Tags", Icons.AutoMirrored.Filled.Label)
    LandingScreen.Reviews -> LandingScreenDetails("Reviews", Icons.Filled.RateReview)
    LandingScreen.Stats -> LandingScreenDetails("Stats", Icons.AutoMirrored.Filled.FactCheck)
}

data class LandingScreenDetails(
    val title: String,
    val icon: ImageVector,
)
