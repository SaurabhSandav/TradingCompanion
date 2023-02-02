package com.saurabhsandav.core

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.landing.LandingScreen
import com.saurabhsandav.core.ui.theme.AppTheme
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys

@Composable
@Preview
internal fun App() {

    val appModule = LocalAppModule.current
    val useDarkTheme by remember {
        appModule.appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
    }.collectAsState(PrefDefaults.DarkModeEnabled)

    AppTheme(useDarkTheme = useDarkTheme) {

        Surface {
            LandingScreen()
        }
    }
}

fun main() = application {

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val appModule = remember { AppModule() }

    val densityFraction by appModule.appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
        .collectAsState(PrefDefaults.DensityFraction)

    CompositionLocalProvider(
        LocalDensityFraction provides densityFraction,
        LocalAppModule provides appModule,
    ) {

        AppWindow(
            onCloseRequest = ::exitApplication,
            state = windowState,
        ) {

            val appWindowState = LocalAppWindowState.current

            // Set window title inside window scope, so it can be overridden in child composables
            LaunchedEffect(appWindowState) { appWindowState.title = "Trading Companion" }

            App()
        }
    }
}

internal val LocalDensityFraction = staticCompositionLocalOf { PrefDefaults.DensityFraction }

internal val LocalAppModule = staticCompositionLocalOf<AppModule> { error("AppModule is not provided") }
