package com.saurabhsandav.core.ui.settings.backup

import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormGraph
import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface BackupSettingsGraph {

    val presenterFactory: BackupSettingsPresenter.Factory

    val backupServiceFormGraphFactory: BackupServiceFormGraph.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): BackupSettingsGraph
    }
}
