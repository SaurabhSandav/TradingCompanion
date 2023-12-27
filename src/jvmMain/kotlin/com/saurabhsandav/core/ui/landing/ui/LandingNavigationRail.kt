package com.saurabhsandav.core.ui.landing.ui

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher

@Composable
internal fun LandingNavigationRail(
    currentScreen: LandingState.LandingScreen,
    onCurrentScreenChange: (LandingState.LandingScreen) -> Unit,
    tradeContentLauncher: TradeContentLauncher,
    openTradesCount: Long?,
    onOpenProfiles: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {

        NavigationRailItem(
            icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profiles") },
            selected = false,
            onClick = onOpenProfiles,
        )

        Divider(Modifier.align(Alignment.CenterHorizontally).width(64.dp))

        val landingItems = remember { enumValues<LandingState.LandingScreen>() }

        landingItems.forEach { screen ->

            TooltipArea(
                tooltip = {

                    val text = when (screen) {
                        LandingState.LandingScreen.Trades -> "${screen.title} - $openTradesCount open trades"
                        else -> screen.title
                    }

                    Tooltip(text)
                },
            ) {

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
                    onClick = { onCurrentScreenChange(screen) }
                )
            }
        }

        Divider(Modifier.align(Alignment.CenterHorizontally).width(64.dp))

        TooltipArea(
            tooltip = { Tooltip("Charts") },
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.CandlestickChart, contentDescription = "Charts") },
                selected = false,
                onClick = tradeContentLauncher::openCharts,
            )
        }

        TooltipArea(
            tooltip = { Tooltip("Tags") },
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Default.Label, contentDescription = "Tags") },
                selected = false,
                onClick = onOpenTags,
            )
        }

        TooltipArea(
            tooltip = { Tooltip("PNL Calculator") },
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Calculate, contentDescription = "PNL Calculator") },
                selected = false,
                onClick = onOpenPnlCalculator,
            )
        }

        TooltipArea(
            tooltip = { Tooltip("Bar Replay") },
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Replay, contentDescription = "Bar Replay") },
                selected = false,
                onClick = onOpenBarReplay,
            )
        }

        TooltipArea(
            tooltip = { Tooltip("Settings") },
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }
}
