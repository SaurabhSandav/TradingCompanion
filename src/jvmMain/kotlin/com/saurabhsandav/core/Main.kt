package com.saurabhsandav.core

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.LandingScreen
import com.saurabhsandav.core.ui.theme.AppTheme
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys

fun main() = application {

    val appModule = remember { AppModule() }

    val densityFraction by appModule.appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
        .collectAsState(PrefDefaults.DensityFraction)

    CompositionLocalProvider(
        LocalDensityFraction provides densityFraction,
        LocalAppModule provides appModule,
    ) {

        App(::exitApplication)
    }
}

@Composable
@Preview
internal fun App(onCloseRequest: () -> Unit) {

    val windowState = rememberAppWindowState(
        windowState = rememberWindowState(placement = WindowPlacement.Maximized),
        defaultTitle = "Trading Companion",
    )

    var showExitConfirmationDialog by state { false }

    AppWindow(
        onCloseRequest = { showExitConfirmationDialog = true },
        state = windowState,
    ) {

        val appModule = LocalAppModule.current
        val useDarkTheme by remember {
            appModule.appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        }.collectAsState(PrefDefaults.DarkModeEnabled)

        AppTheme(useDarkTheme = useDarkTheme) {

            Surface {

                LandingScreen()

                if (showExitConfirmationDialog) {

                    ExitConfirmationDialog(
                        onDismiss = { showExitConfirmationDialog = false },
                        onConfirm = onCloseRequest,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text("Are you sure you want to exit?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}

internal val LocalDensityFraction = staticCompositionLocalOf { PrefDefaults.DensityFraction }

internal val LocalAppModule = staticCompositionLocalOf<AppModule> { error("AppModule is not provided") }
