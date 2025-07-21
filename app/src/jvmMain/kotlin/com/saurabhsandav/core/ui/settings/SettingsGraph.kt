package com.saurabhsandav.core.ui.settings

import com.saurabhsandav.core.ui.settings.backup.BackupSettingsGraph
import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface SettingsGraph {

    val presenterFactory: SettingsPresenter.Factory

    val backupSettingsGraphFactory: BackupSettingsGraph.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): SettingsGraph
    }
}
