package com.saurabhsandav.core

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
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

    val windowState = rememberAppWindowState(
        windowState = rememberWindowState(placement = WindowPlacement.Maximized),
        defaultTitle = "Trading Companion",
    )
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

            App()
        }
    }
}

internal val LocalDensityFraction = staticCompositionLocalOf { PrefDefaults.DensityFraction }

internal val LocalAppModule = staticCompositionLocalOf<AppModule> { error("AppModule is not provided") }
