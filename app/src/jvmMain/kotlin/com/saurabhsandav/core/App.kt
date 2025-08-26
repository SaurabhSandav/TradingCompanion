package com.saurabhsandav.core

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.application
import app.cash.molecule.SnapshotNotifier
import co.touchlab.kermit.Logger
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.di.ScreensModule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.barreplay.BarReplayWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.LandingWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.profiles.ProfilesWindow
import com.saurabhsandav.core.ui.settings.SettingsWindow
import com.saurabhsandav.core.ui.theme.AppTheme
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Toolkit
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

suspend fun runApp(isDebugMode: Boolean) {

    setWM_CLASS()

    // For Molecule. Read `SnapshotNotifier` docs for more.
    System.setProperty("app.cash.molecule.snapshotNotifier", SnapshotNotifier.External.name)

    val restoreScheduler = RestoreScheduler()

    restoreScheduler.restoreAndRestartScope {

        val appModule = AppModule(isDebugMode, restoreScheduler)
        val appConfig = appModule.appConfig
        val initialLandingProfileId = appConfig.getCurrentTradingProfile().id

        runApp(
            onExit = { appModule.startupManager.destroy() },
            appModule = appModule,
            screensModule = appModule.screensModule,
            appConfig = appConfig,
            restoreScheduler = restoreScheduler,
            initialLandingProfileId = initialLandingProfileId,
            backupManager = appModule.backupManager,
            tradeContentLauncher = appModule.tradeContentLauncher,
        )
    }
}

private fun runApp(
    onExit: () -> Unit,
    appModule: AppModule,
    screensModule: ScreensModule,
    appConfig: AppConfig,
    restoreScheduler: RestoreScheduler,
    initialLandingProfileId: ProfileId,
    backupManager: BackupManager,
    tradeContentLauncher: TradeContentLauncher,
) {

    application(exitProcessOnExit = false) {

        val onExitApplication = remember {
            {
                onExit()
                exitApplication()
            }
        }

        LaunchedEffect(Unit) {
            restoreScheduler.init(backupManager, onExitApplication)
        }

        CompositionLocalProvider(
            LocalAppConfig provides appConfig,
            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
            LocalAppModule provides appModule,
            LocalScreensModule provides screensModule,
        ) {

            App(
                onExit = {
                    onExitApplication()

                    // App process doesn't exit if browser(charts) was initialized at any point.
                    // Workaround: Wait 10 seconds (arbitrary) and force exit app.
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch {
                        delay(10.seconds)
                        exitProcess(0)
                    }
                },
                initialLandingProfileId = initialLandingProfileId,
                tradeContentLauncher = tradeContentLauncher,
            )
        }
    }
}

@Composable
private fun App(
    onExit: () -> Unit,
    initialLandingProfileId: ProfileId,
    tradeContentLauncher: TradeContentLauncher,
) {

    val appConfig = LocalAppConfig.current

    AppTheme(useDarkTheme = appConfig.isDarkModeEnabled) {

        val landingWindowsManager = remember { AppWindowsManager(listOf(initialLandingProfileId)) }
        val profilesWindowManager = remember { AppWindowManager() }
        val pnlCalculatorWindowManager = remember { AppWindowManager() }
        val barReplayWindowManager = remember { AppWindowManager() }
        val settingsWindowManager = remember { AppWindowManager() }

        landingWindowsManager.Windows { window ->

            LandingWindow(
                onCloseRequest = {

                    when {
                        landingWindowsManager.windows.size == 1 -> onExit()
                        else -> window.close()
                    }
                },
                closeExitsApp = landingWindowsManager.windows.size == 1,
                profileId = window.params,
                onOpenProfiles = profilesWindowManager::openWindow,
                onOpenPnlCalculator = pnlCalculatorWindowManager::openWindow,
                onOpenBarReplay = barReplayWindowManager::openWindow,
                onOpenSettings = settingsWindowManager::openWindow,
            )
        }

        // Trade content windows
        tradeContentLauncher.Windows()

        // Profiles
        profilesWindowManager.Window {

            ProfilesWindow(
                onCloseRequest = profilesWindowManager::closeWindow,
                onSelectProfile = { profileId ->

                    when (val landingWindow = landingWindowsManager.windows.find { it.params == profileId }) {

                        // Open new window
                        null -> landingWindowsManager.newWindow(profileId)

                        // Window already open. Bring to front.
                        else -> landingWindow.toFront()
                    }
                },
            )
        }

        // PNL Calculator
        pnlCalculatorWindowManager.Window {

            PNLCalculatorWindow(
                onCloseRequest = pnlCalculatorWindowManager::closeWindow,
            )
        }

        // Bar Replay
        barReplayWindowManager.Window {

            BarReplayWindow(
                onCloseRequest = barReplayWindowManager::closeWindow,
                onOpenProfile = landingWindowsManager::newWindow,
            )
        }

        // Settings
        settingsWindowManager.Window {

            SettingsWindow(
                onCloseRequest = settingsWindowManager::closeWindow,
            )
        }
    }
}

@Suppress("FunctionName")
private fun setWM_CLASS() {

    val xToolkit = Toolkit.getDefaultToolkit()

    try {

        xToolkit.javaClass.getDeclaredField("awtAppClassName").apply {
            isAccessible = true
            set(xToolkit, "Trading Companion")
        }
    } catch (_: Exception) {
        Logger.d { "Could not set WM_CLASS" }
    }
}

internal val LocalAppConfig = staticCompositionLocalOf<AppConfig> { error("AppConfig is not provided") }

internal val LocalAppModule = staticCompositionLocalOf<AppModule> { error("AppModule is not provided") }
internal val LocalScreensModule = staticCompositionLocalOf<ScreensModule> { error("ScreensModule is not provided") }
