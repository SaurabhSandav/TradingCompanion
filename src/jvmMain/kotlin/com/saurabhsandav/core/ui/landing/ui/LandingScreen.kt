package com.saurabhsandav.core.ui.landing.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.account.AccountLandingSwitcherItem
import com.saurabhsandav.core.ui.common.WindowsOnlyLayout
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.*
import com.saurabhsandav.core.ui.reviews.ReviewsLandingSwitcherItem
import com.saurabhsandav.core.ui.sizing.SizingLandingSwitcherItem
import com.saurabhsandav.core.ui.studies.StudiesLandingSwitcherItem
import com.saurabhsandav.core.ui.tags.TagsWindow
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsLandingSwitcherItem
import com.saurabhsandav.core.ui.trades.TradesLandingSwitcherItem

@Composable
internal fun LandingScreen(
    profileId: ProfileId,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.landingModule(scope, profileId).presenter() }
    val state by presenter.state.collectAsState()

    val switcherItems = remember {
        mapOf(
            Account to AccountLandingSwitcherItem(appModule.accountModule(scope)),
            TradeSizing to SizingLandingSwitcherItem(appModule.sizingModule(scope, profileId)),
            TradeExecutions to TradeExecutionsLandingSwitcherItem(appModule.tradeExecutionsModule(scope, profileId)),
            Trades to TradesLandingSwitcherItem(appModule.tradesModule(scope, profileId)),
            Reviews to ReviewsLandingSwitcherItem(appModule.reviewsModule(scope, profileId)),
            Studies to StudiesLandingSwitcherItem(appModule.studiesModule(scope, profileId)),
        )
    }

    val currentScreen = state.currentScreen

    if (currentScreen != null) {

        LandingScreen(
            profileId = profileId,
            switcherItems = switcherItems,
            currentScreen = currentScreen,
            onCurrentScreenChange = { state.eventSink(LandingEvent.ChangeCurrentScreen(it)) },
            tradeContentLauncher = appModule.tradeContentLauncher,
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
    profileId: ProfileId,
    switcherItems: Map<LandingScreen, LandingSwitcherItem>,
    currentScreen: LandingScreen,
    onCurrentScreenChange: (LandingScreen) -> Unit,
    tradeContentLauncher: TradeContentLauncher,
    openTradesCount: Long?,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    val tagsWindowManager = remember { AppWindowManager() }

    Row {

        // Navigation Rail
        LandingNavigationRail(
            currentScreen = currentScreen,
            onCurrentScreenChange = onCurrentScreenChange,
            tradeContentLauncher = tradeContentLauncher,
            openTradesCount = openTradesCount,
            onOpenTags = tagsWindowManager::openWindow,
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

        // Tags
        tagsWindowManager.Window {

            TagsWindow(
                onCloseRequest = tagsWindowManager::closeWindow,
                profileId = profileId,
            )
        }
    }
}
