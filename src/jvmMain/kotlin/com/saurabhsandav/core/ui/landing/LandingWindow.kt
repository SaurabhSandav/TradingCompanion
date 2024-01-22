package com.saurabhsandav.core.ui.landing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.ui.LandingScreen
import kotlinx.coroutines.flow.map

@Composable
internal fun LandingWindow(
    onCloseRequest: () -> Unit,
    closeExitsApp: Boolean,
    profileId: ProfileId,
    onOpenProfiles: () -> Unit,
    onOpenPnlCalculator: () -> Unit,
    onOpenBarReplay: () -> Unit,
    onOpenSettings: () -> Unit,
) {

    val appModule = LocalAppModule.current
    val profileName by remember {
        appModule.tradingProfiles.getProfile(profileId).map { "${it.name} - " }
    }.collectAsState("")

    val windowState = rememberAppWindowState(
        preferredPlacement = WindowPlacement.Maximized,
        defaultTitle = "Trading Companion",
        titleTransform = { title -> "$profileName$title" },
    )

    var showExitConfirmationDialog by state { false }

    AppWindow(
        onCloseRequest = {
            when {
                closeExitsApp -> {
                    windowState.toFront()
                    showExitConfirmationDialog = true
                }

                else -> onCloseRequest()
            }
        },
        state = windowState,
    ) {

        LandingScreen(
            profileId = profileId,
            onOpenProfiles = onOpenProfiles,
            onOpenPnlCalculator = onOpenPnlCalculator,
            onOpenBarReplay = onOpenBarReplay,
            onOpenSettings = onOpenSettings,
        )

        if (showExitConfirmationDialog) {

            ExitConfirmationDialog(
                onDismiss = { showExitConfirmationDialog = false },
                onConfirm = onCloseRequest,
            )
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
