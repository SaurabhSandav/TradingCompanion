package com.saurabhsandav.core.ui.settings.backup.serviceform

import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface BackupServiceFormGraph {

    val presenterFactory: BackupServiceFormPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides formType: BackupServiceFormType,
        ): BackupServiceFormGraph
    }
}
