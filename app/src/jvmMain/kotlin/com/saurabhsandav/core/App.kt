package com.saurabhsandav.core

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.di.ScreensModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.barreplay.BarReplayWindow
import com.saurabhsandav.core.ui.common.app.AppWindowManager
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.LandingWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindow
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams
import com.saurabhsandav.core.ui.pnlcalculator.rememberPNLCalculatorWindowState
import com.saurabhsandav.core.ui.profiles.ProfilesWindow
import com.saurabhsandav.core.ui.settings.SettingsWindow
import com.saurabhsandav.core.ui.theme.AppTheme
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import kotlinx.coroutines.flow.first
import java.awt.Toolkit

suspend fun runApp(
    isDebugMode: Boolean,
) {

    setWM_CLASS()

    val restoreScheduler = RestoreScheduler()

    restoreScheduler.withRestoreScope {

        val appModule = AppModule(isDebugMode, restoreScheduler)
        val initialLandingProfileId = appModule.appPrefs.getCurrentTradingProfile(appModule.tradingProfiles).first().id

        application(exitProcessOnExit = false) {

            val onExit = remember {
                {
                    appModule.destroy()
                    exitApplication()
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
            setAccessible(true)
            set(xToolkit, "Trading Companion")
        }
    } catch (e: Exception) {
        Logger.e(e) { "Could not set WM_CLASS" }
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

        // Login windows
        appModule.loginServicesManager.Windows()

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
                state = rememberPNLCalculatorWindowState(
                    params = PNLCalculatorWindowParams(
                        operationType = PNLCalculatorWindowParams.OperationType.New,
                        onCloseRequest = pnlCalculatorWindowManager::closeWindow,
                    )
                )
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
