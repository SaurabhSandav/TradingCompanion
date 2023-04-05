package com.saurabhsandav.core.ui.landing

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.account.AccountPresenter
import com.saurabhsandav.core.ui.account.AccountScreen
import com.saurabhsandav.core.ui.barreplay.BarReplayWindow
import com.saurabhsandav.core.ui.charts.ChartsScreen
import com.saurabhsandav.core.ui.closedtrades.ClosedTradesPresenter
import com.saurabhsandav.core.ui.closedtrades.ClosedTradesScreen
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.opentrades.OpenTradesPresenter
import com.saurabhsandav.core.ui.opentrades.OpenTradesScreen
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams
import com.saurabhsandav.core.ui.pnlcalculator.rememberPNLCalculatorWindowState
import com.saurabhsandav.core.ui.settings.SettingsWindow
import com.saurabhsandav.core.ui.sizing.SizingPresenter
import com.saurabhsandav.core.ui.sizing.SizingScreen
import com.saurabhsandav.core.ui.studies.StudiesPresenter
import com.saurabhsandav.core.ui.studies.StudiesScreen
import com.saurabhsandav.core.ui.tradeorders.TradeOrdersPresenter
import com.saurabhsandav.core.ui.tradeorders.TradeOrdersScreen
import com.saurabhsandav.core.ui.trades.TradesPresenter
import com.saurabhsandav.core.ui.trades.TradesScreen

@Composable
internal fun LandingScreen() {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { LandingPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    val currentScreen = state.currentScreen

    if (currentScreen != null) {

        LandingScreen(
            appModule = appModule,
            currentScreen = currentScreen,
            onCurrentScreenChange = { presenter.event(LandingEvent.ChangeCurrentScreen(it)) },
        )
    }
}

@Composable
private fun LandingScreen(
    appModule: AppModule,
    currentScreen: LandingScreen,
    onCurrentScreenChange: (LandingScreen) -> Unit,
) {

    var showChartsWindow by state { false }
    val chartsWindowOwner = remember { AppWindowOwner() }

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

            val landingItems = remember {
                enumValues<LandingScreen>().filter {
                    it != LandingScreen.OpenTrades && it != LandingScreen.ClosedTrades
                }
            }

            landingItems.forEach { screen ->

                TooltipArea(
                    tooltip = { Tooltip(screen.title) },
                ) {

                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
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

        Box(Modifier.fillMaxSize()) {

            val coroutineScope = rememberCoroutineScope()

            val accountPresenter = remember { AccountPresenter(coroutineScope, appModule) }
            val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
            val openTradesPresenter = remember { OpenTradesPresenter(coroutineScope, appModule) }
            val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }
            val tradeOrdersPresenter = remember { TradeOrdersPresenter(coroutineScope, appModule) }
            val tradesPresenter = remember { TradesPresenter(coroutineScope, appModule) }
            val studiesPresenter = remember { StudiesPresenter(coroutineScope, appModule) }

            AnimatedContent(currentScreen) { targetState ->

                when (targetState) {
                    LandingScreen.Account -> AccountScreen(accountPresenter)
                    LandingScreen.TradeSizing -> SizingScreen(sizingPresenter)
                    LandingScreen.OpenTrades -> OpenTradesScreen(openTradesPresenter)
                    LandingScreen.ClosedTrades -> ClosedTradesScreen(closedTradesPresenter)
                    LandingScreen.TradeOrders -> TradeOrdersScreen(tradeOrdersPresenter)
                    LandingScreen.Trades -> TradesScreen(tradesPresenter)
                    LandingScreen.Studies -> StudiesScreen(studiesPresenter)
                }
            }
        }

        if (showChartsWindow) {

            AppWindowOwner(chartsWindowOwner) {

                ChartsScreen(
                    onCloseRequest = { showChartsWindow = false },
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
