package com.saurabhsandav.core

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.awt.Toolkit
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

suspend fun runApp(isDebugMode: Boolean) {

    setWM_CLASS()

    // For Molecule. Read `SnapshotNotifier` docs for more.
    System.setProperty("app.cash.molecule.snapshotNotifier", SnapshotNotifier.External.name)

    val restoreScheduler = RestoreScheduler()

    restoreScheduler.withRestoreScope {

        val appModule = AppModule(isDebugMode, restoreScheduler)
        val initialLandingProfileId = appModule.appPrefs.getCurrentTradingProfile(appModule.tradingProfiles).first().id

        application(exitProcessOnExit = false) {

            val onExit = remember<() -> Unit> {
                {
                    appModule.startupManager.destroy()
                    exitApplication()

                    // App process doesn't exit if browser(charts) was initialized at any point.
                    // Workaround: Wait 10 seconds (arbitrary) and force exit app.
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch {
                        delay(10.seconds)
                        exitProcess(0)
                    }
                }
            }

            LaunchedEffect(Unit) {
                restoreScheduler.init(appModule.backupManager, onExit)
            }

            CompositionLocalProvider(
                LocalAppConfig provides appModule.appConfig,
                LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                LocalAppModule provides appModule,
                LocalScreensModule provides appModule.screensModule,
            ) {

                App(
                    onCloseRequest = onExit,
                    initialLandingProfileId = initialLandingProfileId,
                    isDarkModeEnabled = appModule.appConfig.isDarkModeEnabled,
                )
            }
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

@Composable
@Preview
internal fun App(
    onCloseRequest: () -> Unit,
    initialLandingProfileId: ProfileId,
    isDarkModeEnabled: Boolean,
) {

    val appModule = LocalAppModule.current

    AppTheme(useDarkTheme = isDarkModeEnabled) {

        val landingWindowsManager = remember { AppWindowsManager(listOf(initialLandingProfileId)) }
        val profilesWindowManager = remember { AppWindowManager() }
        val pnlCalculatorWindowManager = remember { AppWindowManager() }
        val barReplayWindowManager = remember { AppWindowManager() }
        val settingsWindowManager = remember { AppWindowManager() }

        landingWindowsManager.Windows { window ->

            LandingWindow(
                onCloseRequest = {

                    when {
                        landingWindowsManager.windows.size == 1 -> onCloseRequest()
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
        appModule.tradeContentLauncher.Windows()

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

internal val LocalAppConfig = staticCompositionLocalOf<AppConfig> { error("AppConfig is not provided") }

internal val LocalAppModule = staticCompositionLocalOf<AppModule> { error("AppModule is not provided") }
internal val LocalScreensModule = staticCompositionLocalOf<ScreensModule> { error("ScreensModule is not provided") }
