// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow
import ui.landing.LandingScreen
import ui.theme.AppTheme
import utils.PrefDefaults
import utils.PrefKeys

@Composable
@Preview
internal fun App(appModule: AppModule) {

    AppTheme(useDarkTheme = false) {

        LandingScreen(appModule)
    }
}

fun main() = application {

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val appModule = remember { AppModule() }

    val densityFraction by appModule.appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
        .collectAsState(PrefDefaults.DensityFraction)

    CompositionLocalProvider(LocalDensityFraction provides densityFraction) {

        AppWindow(
            onCloseRequest = ::exitApplication,
            state = windowState,
        ) {

            App(appModule)
        }
    }
}

internal val LocalDensityFraction = staticCompositionLocalOf { PrefDefaults.DensityFraction }
