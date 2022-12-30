package ui.landing

import AppModule
import LocalAppModule
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.account.AccountScreen
import ui.barreplay.BarReplayWindow
import ui.charts.ChartsWindow
import ui.closedtrades.ClosedTradesPresenter
import ui.closedtrades.ClosedTradesScreen
import ui.common.Tooltip
import ui.common.state
import ui.landing.model.LandingEvent
import ui.landing.model.LandingScreen
import ui.opentrades.OpenTradesPresenter
import ui.opentrades.OpenTradesScreen
import ui.settings.SettingsWindow
import ui.sizing.SizingPresenter
import ui.sizing.SizingScreen
import ui.studies.StudiesPresenter
import ui.studies.StudiesScreen

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
    var showBarReplayWindow by state { false }
    var showSettingsWindow by state { false }

    Row {

        NavigationRail(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {

            LandingScreen.items.forEach { screen ->

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
                    onClick = { showChartsWindow = true }
                )
            }

            TooltipArea(
                tooltip = { Tooltip("Bar Replay") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Replay, contentDescription = "Bar Replay") },
                    selected = false,
                    onClick = { showBarReplayWindow = true }

                )
            }

            TooltipArea(
                tooltip = { Tooltip("Settings") },
            ) {

                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    selected = false,
                    onClick = { showSettingsWindow = true }
                )
            }
        }

        Box(Modifier.fillMaxSize()) {

            val coroutineScope = rememberCoroutineScope()

            val accountPresenter = remember { ui.account.AccountPresenter(coroutineScope, appModule) }
            val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
            val openTradesPresenter = remember { OpenTradesPresenter(coroutineScope, appModule) }
            val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }
            val studiesPresenter = remember { StudiesPresenter(coroutineScope, appModule) }

            AnimatedContent(currentScreen) { targetState ->

                when (targetState) {
                    LandingScreen.Account -> AccountScreen(accountPresenter)
                    LandingScreen.TradeSizing -> SizingScreen(sizingPresenter)
                    LandingScreen.OpenTrades -> OpenTradesScreen(openTradesPresenter)
                    LandingScreen.ClosedTrades -> ClosedTradesScreen(closedTradesPresenter)
                    LandingScreen.Studies -> StudiesScreen(studiesPresenter)
                }
            }
        }

        if (showChartsWindow) {

            ChartsWindow(
                onCloseRequest = { showChartsWindow = false },
            )
        }

        if (showBarReplayWindow) {

            BarReplayWindow(
                onCloseRequest = { showBarReplayWindow = false },
            )
        }

        if (showSettingsWindow) {

            SettingsWindow(
                onCloseRequest = { showSettingsWindow = false },
            )
        }
    }
}
