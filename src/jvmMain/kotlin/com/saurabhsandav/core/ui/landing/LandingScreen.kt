package com.saurabhsandav.core.ui.landing

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.account.AccountLandingSwitcherItem
import com.saurabhsandav.core.ui.barreplay.BarReplayWindow
import com.saurabhsandav.core.ui.common.WindowsOnlyLayout
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.core.ui.landing.ui.LandingNavigationRail
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams
import com.saurabhsandav.core.ui.pnlcalculator.rememberPNLCalculatorWindowState
import com.saurabhsandav.core.ui.profiles.ProfilesWindow
import com.saurabhsandav.core.ui.reviews.ReviewsLandingSwitcherItem
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
            LandingScreen.Reviews to ReviewsLandingSwitcherItem(appModule.reviewsModule(scope)),
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
    currentScreen: LandingScreen,
    onCurrentScreenChange: (LandingScreen) -> Unit,
    tradeContentLauncher: TradeContentLauncher,
    openTradesCount: Long?,
) {

    val profilesWindowManager = remember { AppWindowManager() }
    val tagsWindowManager = remember { AppWindowManager() }
    val pnlCalculatorWindowManager = remember { AppWindowManager() }
    val barReplayWindowManager = remember { AppWindowManager() }
    val settingsWindowManager = remember { AppWindowManager() }

    Row {

        LandingNavigationRail(
            currentScreen = currentScreen,
            onCurrentScreenChange = onCurrentScreenChange,
            tradeContentLauncher = tradeContentLauncher,
            openTradesCount = openTradesCount,
            onOpenProfiles = profilesWindowManager::openWindow,
            onOpenTags = tagsWindowManager::openWindow,
            onOpenPnlCalculator = pnlCalculatorWindowManager::openWindow,
            onOpenBarReplay = barReplayWindowManager::openWindow,
            onOpenSettings = settingsWindowManager::openWindow,
        )

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
