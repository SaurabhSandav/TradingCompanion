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
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.state
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

    val currentScreen = state.currentScreen

    if (currentScreen != null) {

        LandingScreen(
            currentScreen = currentScreen,
            openTradesCount = state.openTradesCount,
            onCurrentScreenChange = { state.eventSink(LandingEvent.ChangeCurrentScreen(it)) },
        )
    }
}

@Composable
private fun LandingScreen(
    currentScreen: LandingScreen,
    openTradesCount: Long?,
    onCurrentScreenChange: (LandingScreen) -> Unit,
) {

    var showProfilesWindow by state { false }
    val profilesWindowOwner = remember { AppWindowOwner() }

    var showChartsWindow by state { false }
    val chartsWindowOwner = remember { AppWindowOwner() }

    var showTagsWindow by state { false }
    val tagsWindowOwner = remember { AppWindowOwner() }

    var showPNLCalculatorWindow by state { false }
    val pnlCalculatorWindowOwner = remember { AppWindowOwner() }

    var showBarReplayWindow by state { false }
    val barReplayWindowOwner = remember { AppWindowOwner() }

    var showSettingsWindow by state { false }
    val settingsWindowOwner = remember { AppWindowOwner() }

    Row {

        NavigationRail(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {

            NavigationRailItem(
                icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profiles") },
                selected = false,
                onClick = {
                    showProfilesWindow = true
                    profilesWindowOwner.childrenToFront()
                }
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
                    onClick = {
                        showChartsWindow = true
                        chartsWindowOwner.childrenToFront()
                    }
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Tags") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = "Tags") },
                    selected = false,
                    onClick = {
                        showTagsWindow = true
                        tagsWindowOwner.childrenToFront()
                    }
                )
            }

            TooltipArea(
                tooltip = { Tooltip("PNL Calculator") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = "PNL Calculator") },
                    selected = false,
                    onClick = {
                        showPNLCalculatorWindow = true
                        pnlCalculatorWindowOwner.childrenToFront()
                    }
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Bar Replay") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Replay, contentDescription = "Bar Replay") },
                    selected = false,
                    onClick = {
                        showBarReplayWindow = true
                        barReplayWindowOwner.childrenToFront()
                    }
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Settings") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    selected = false,
                    onClick = {
                        showSettingsWindow = true
                        settingsWindowOwner.childrenToFront()
                    }
                )
            }
        }

        val tradeContentLauncher = remember { TradeContentLauncher() }

        // Trade content windows
        WindowsOnlyLayout {

            tradeContentLauncher.Windows()
        }

        Box(Modifier.fillMaxSize()) {

            val appModule = LocalAppModule.current
            val scope = rememberCoroutineScope()

            val switcherItems = remember {
                mapOf(
                    LandingScreen.Account to AccountLandingSwitcherItem(appModule.accountModule(scope)),
                    LandingScreen.TradeSizing to SizingLandingSwitcherItem(appModule.sizingModule(scope)),
                    LandingScreen.TradeExecutions to TradeExecutionsLandingSwitcherItem(
                        appModule.tradeExecutionsModule(scope, tradeContentLauncher)
                    ),
                    LandingScreen.Trades to TradesLandingSwitcherItem(
                        appModule.tradesModule(scope, tradeContentLauncher)
                    ),
                    LandingScreen.Studies to StudiesLandingSwitcherItem(appModule.studiesModule(scope)),
                )
            }

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

        if (showProfilesWindow) {

            AppWindowOwner(profilesWindowOwner) {

                ProfilesWindow(
                    onCloseRequest = { showProfilesWindow = false },
                )
            }
        }

        if (showChartsWindow) {

            AppWindowOwner(chartsWindowOwner) {

                ChartsScreen(
                    tradeContentLauncher = tradeContentLauncher,
                    onCloseRequest = { showChartsWindow = false },
                )
            }
        }

        if (showTagsWindow) {

            AppWindowOwner(tagsWindowOwner) {

                TagsWindow(
                    onCloseRequest = { showTagsWindow = false },
                )
            }
        }

        if (showPNLCalculatorWindow) {

            AppWindowOwner(pnlCalculatorWindowOwner) {

                PNLCalculatorWindow(
                    state = rememberPNLCalculatorWindowState(
                        params = PNLCalculatorWindowParams(
                            operationType = PNLCalculatorWindowParams.OperationType.New,
                            onCloseRequest = { showPNLCalculatorWindow = false },
                        )
                    )
                )
            }
        }

        if (showBarReplayWindow) {

            AppWindowOwner(barReplayWindowOwner) {

                BarReplayWindow(
                    onCloseRequest = { showBarReplayWindow = false },
                )
            }
        }

        if (showSettingsWindow) {

            AppWindowOwner(settingsWindowOwner) {

                SettingsWindow(
                    onCloseRequest = { showSettingsWindow = false },
                )
            }
        }
    }
}
