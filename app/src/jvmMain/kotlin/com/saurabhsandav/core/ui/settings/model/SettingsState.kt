package com.saurabhsandav.core.ui.settings.model

import com.saurabhsandav.core.backup.BackupItem
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen

internal data class SettingsState(
    val darkModeEnabled: Boolean,
    val landingScreen: LandingScreen,
    val densityFraction: Float,
    val defaultTimeframe: Timeframe,
    val backupProgress: BackupProgress?,
    val eventSink: (SettingsEvent) -> Unit,
) {

    sealed class BackupProgress {

        data class GeneratingArchive(
            val item: BackupItem,
            val progress: Float,
        ) : BackupProgress()

        data object SavingArchive : BackupProgress()
    }
}
