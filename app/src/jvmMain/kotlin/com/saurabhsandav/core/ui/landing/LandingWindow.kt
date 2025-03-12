package com.saurabhsandav.core.ui.landing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.ui.LandingScreen
import kotlinx.coroutines.flow.transform

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
        appModule.tradingProfiles.getProfileOrNull(profileId).transform { profile ->

            if (profile == null) {
                onCloseRequest()
                return@transform
            }

            emit("${profile.name} - ")
        }
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

            ConfirmationDialog(
                text = "Are you sure you want to exit?",
                onDismiss = { showExitConfirmationDialog = false },
                onConfirm = onCloseRequest,
            )
        }
    }
}
