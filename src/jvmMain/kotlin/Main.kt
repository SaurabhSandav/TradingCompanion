// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.main.MainScreen
import ui.theme.AppTheme

@Composable
@Preview
fun App() {

    AppTheme(useDarkTheme = false) {

        val appModule = remember { AppModule() }

        MainScreen(appModule)
    }
}

fun main() = application {

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
    ) {

        val density = LocalDensity.current

        val newDensity = Density(density.density * AppDensityFraction, density.fontScale)

        CompositionLocalProvider(LocalDensity provides newDensity) {
            App()
        }
    }
}

const val AppDensityFraction = 0.8F
