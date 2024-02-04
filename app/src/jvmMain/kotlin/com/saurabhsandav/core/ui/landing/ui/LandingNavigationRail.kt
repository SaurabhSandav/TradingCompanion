package com.saurabhsandav.core.ui.landing.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher

@Composable
internal fun LandingNavigationRail(
    currentScreen: LandingState.LandingScreen,
    onCurrentScreenChange: (LandingState.LandingScreen) -> Unit,
    tradeContentLauncher: TradeContentLauncher,
    openTradesCount: Int?,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenAutoTrader: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {

        val landingItems = remember { enumValues<LandingState.LandingScreen>() }

        landingItems.forEach { screen ->

            val tooltipText = when {
                screen == LandingState.LandingScreen.Trades &&
                    openTradesCount != null -> "${screen.title} - $openTradesCount open trades"

                else -> screen.title
            }

            SimpleTooltipBox(tooltipText) {

                NavigationRailItem(
                    icon = {

                        BadgedBox(
                            badge = {

                                if (screen == LandingState.LandingScreen.Trades && openTradesCount != null) {

                                    Badge {

                                        Text(
                                            modifier = Modifier.semantics {
                                                contentDescription = "$openTradesCount open trades"
                                            },
                                            text = openTradesCount.toString(),
                                        )
                                    }
                                }
                            },
                            content = { Icon(screen.icon, contentDescription = screen.title) },
                        )
                    },
                    selected = currentScreen == screen,
                    onClick = { onCurrentScreenChange(screen) },
                )
            }
        }

        HorizontalDivider(Modifier.align(Alignment.CenterHorizontally).width(64.dp))

        SimpleTooltipBox("Profiles") {

            NavigationRailItem(
                icon = { Icon(Icons.Default.SwitchAccount, contentDescription = "Profiles") },
                selected = false,
                onClick = onOpenProfiles,
            )
        }

        SimpleTooltipBox("Charts") {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.CandlestickChart, contentDescription = "Charts") },
                selected = false,
                onClick = tradeContentLauncher::openCharts,
            )
        }

        SimpleTooltipBox("PNL Calculator") {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Calculate, contentDescription = "PNL Calculator") },
                selected = false,
                onClick = onOpenPnlCalculator,
            )
        }

        SimpleTooltipBox("Bar Replay") {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Replay, contentDescription = "Bar Replay") },
                selected = false,
                onClick = onOpenBarReplay,
            )
        }

        SimpleTooltipBox("Auto Trader") {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.SmartToy, contentDescription = "Auto Trader") },
                selected = false,
                onClick = onOpenAutoTrader,
            )
        }

        SimpleTooltipBox("Settings") {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }
}
