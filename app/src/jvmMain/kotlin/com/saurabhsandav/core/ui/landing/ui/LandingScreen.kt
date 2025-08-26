package com.saurabhsandav.core.ui.landing.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.WindowsOnlyLayout
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher

@Composable
internal fun LandingScreen(
    profileId: ProfileId,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val landingModule = remember { screensModule.landingModule(scope, profileId) }
    val presenter = remember { landingModule.presenter() }
    val state by presenter.state.collectAsState()

    val currentScreen = state.currentScreen

    if (currentScreen != null) {

        LandingScreen(
            switcherItems = landingModule.switcherItems,
            currentScreen = currentScreen,
            onCurrentScreenChange = { state.eventSink(LandingEvent.ChangeCurrentScreen(it)) },
            tradeContentLauncher = landingModule.tradeContentLauncher,
            openTradesCount = state.openTradesCount,
            onOpenProfiles = onOpenProfiles,
            onOpenPnlCalculator = onOpenPnlCalculator,
            onOpenBarReplay = onOpenBarReplay,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun LandingScreen(
    switcherItems: Map<LandingScreen, LandingSwitcherItem>,
    currentScreen: LandingScreen,
    onCurrentScreenChange: (LandingScreen) -> Unit,
    tradeContentLauncher: TradeContentLauncher,
    openTradesCount: Int?,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    Row {

        // Navigation Rail
        LandingNavigationRail(
            currentScreen = currentScreen,
            onCurrentScreenChange = onCurrentScreenChange,
            tradeContentLauncher = tradeContentLauncher,
            openTradesCount = openTradesCount,
            onOpenProfiles = onOpenProfiles,
            onOpenPnlCalculator = onOpenPnlCalculator,
            onOpenBarReplay = onOpenBarReplay,
            onOpenSettings = onOpenSettings,
        )

        // Main content
        Box(Modifier.fillMaxSize()) {

            val saveableStateHolder = rememberSaveableStateHolder()

            // Main content of currently selected switcher item
            AnimatedContent(currentScreen) { screen ->

                saveableStateHolder.SaveableStateProvider(screen) {

                    remember(screen) { switcherItems.getValue(screen) }.Content()
                }
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
    }
}
