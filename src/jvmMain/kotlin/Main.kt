// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow
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

    AppWindow(
        onCloseRequest = ::exitApplication,
        state = windowState,
    ) {

        App()
    }
}

const val AppDensityFraction = 0.8F
