package com.saurabhsandav.core.ui.landing

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.account.AccountLandingSwitcherItem
import com.saurabhsandav.core.ui.barreplay.BarReplayWindow
import com.saurabhsandav.core.ui.charts.ChartsScreen
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.WindowsOnlyLayout
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams
import com.saurabhsandav.core.ui.pnlcalculator.rememberPNLCalculatorWindowState
import com.saurabhsandav.core.ui.profiles.ProfilesWindow
import com.saurabhsandav.core.ui.settings.SettingsWindow
import com.saurabhsandav.core.ui.sizing.SizingLandingSwitcherItem
import com.saurabhsandav.core.ui.studies.StudiesLandingSwitcherItem
import com.saurabhsandav.core.ui.tags.TagsWindow
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsLandingSwitcherItem
import com.saurabhsandav.core.ui.trades.TradesLandingSwitcherItem

@Composable
internal fun LandingScreen() {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.landingModule(scope).presenter() }
    val state by presenter.state.collectAsState()

    val switcherItems = remember {
        mapOf(
            LandingScreen.Account to AccountLandingSwitcherItem(appModule.accountModule(scope)),
            LandingScreen.TradeSizing to SizingLandingSwitcherItem(appModule.sizingModule(scope)),
            LandingScreen.TradeExecutions to TradeExecutionsLandingSwitcherItem(appModule.tradeExecutionsModule(scope)),
            LandingScreen.Trades to TradesLandingSwitcherItem(appModule.tradesModule(scope)),
            LandingScreen.Studies to StudiesLandingSwitcherItem(appModule.studiesModule(scope)),
        )
    }

    val currentScreen = state.currentScreen

    if (currentScreen != null) {

        LandingScreen(
            switcherItems = switcherItems,
            tradeContentLauncher = appModule.tradeContentLauncher,
            currentScreen = currentScreen,
            openTradesCount = state.openTradesCount,
            onCurrentScreenChange = { state.eventSink(LandingEvent.ChangeCurrentScreen(it)) },
        )
    }
}

@Composable
private fun LandingScreen(
    switcherItems: Map<LandingScreen, LandingSwitcherItem>,
    tradeContentLauncher: TradeContentLauncher,
    currentScreen: LandingScreen,
    openTradesCount: Long?,
    onCurrentScreenChange: (LandingScreen) -> Unit,
) {

    val profilesWindowManager = remember { AppWindowManager() }
    val chartsWindowManager = remember { AppWindowManager() }
    val tagsWindowManager = remember { AppWindowManager() }
    val pnlCalculatorWindowManager = remember { AppWindowManager() }
    val barReplayWindowManager = remember { AppWindowManager() }
    val settingsWindowManager = remember { AppWindowManager() }

    Row {

        NavigationRail(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profiles") },
                selected = false,
                onClick = profilesWindowManager::openWindow,
            )

            Divider(Modifier.align(Alignment.CenterHorizontally).width(64.dp))

            val landingItems = remember { enumValues<LandingScreen>() }

            landingItems.forEach { screen ->

                TooltipArea(
                    tooltip = {

                        val text = when (screen) {
                            LandingScreen.Trades -> "${screen.title} - $openTradesCount open trades"
                            else -> screen.title
                        }

                        Tooltip(text)
                    },
                ) {

                    NavigationRailItem(
                        icon = {

                            BadgedBox(
                                badge = {

                                    if (screen == LandingScreen.Trades && openTradesCount != null) {

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
                    onClick = chartsWindowManager::openWindow,
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Tags") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = "Tags") },
                    selected = false,
                    onClick = tagsWindowManager::openWindow,
                )
            }

            TooltipArea(
                tooltip = { Tooltip("PNL Calculator") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = "PNL Calculator") },
                    selected = false,
                    onClick = pnlCalculatorWindowManager::openWindow,
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Bar Replay") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Replay, contentDescription = "Bar Replay") },
                    selected = false,
                    onClick = barReplayWindowManager::openWindow,
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Settings") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    selected = false,
                    onClick = settingsWindowManager::openWindow,
                )
            }
        }

        // Trade content windows
        WindowsOnlyLayout {

            tradeContentLauncher.Windows()
        }

        Box(Modifier.fillMaxSize()) {

            // Main content of currently selected switcher item
            AnimatedContent(currentScreen) { targetState ->

                remember(targetState) { switcherItems.getValue(targetState) }.Content()
            }

            // Windows of all switcher items
            WindowsOnlyLayout {

                switcherItems.forEach { (key, item) ->

                    key(key) {

                        item.Windows()
                    }
                }
            }
        }


        profilesWindowManager.Window {

            ProfilesWindow(
                onCloseRequest = profilesWindowManager::closeWindow,
            )
        }

        chartsWindowManager.Window {

            ChartsScreen(

                onCloseRequest = chartsWindowManager::closeWindow,
            )
        }

        tagsWindowManager.Window {

            TagsWindow(
                onCloseRequest = tagsWindowManager::closeWindow,
            )
        }

        pnlCalculatorWindowManager.Window {

            PNLCalculatorWindow(
                state = rememberPNLCalculatorWindowState(
                    params = PNLCalculatorWindowParams(
                        operationType = PNLCalculatorWindowParams.OperationType.New,
                        onCloseRequest = pnlCalculatorWindowManager::closeWindow,
                    )
                )
            )
        }

        barReplayWindowManager.Window {

            BarReplayWindow(
                onCloseRequest = barReplayWindowManager::closeWindow,
            )
        }

        settingsWindowManager.Window {

            SettingsWindow(
                onCloseRequest = settingsWindowManager::closeWindow,
            )
        }
    }
}
