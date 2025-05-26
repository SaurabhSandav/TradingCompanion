package com.saurabhsandav.core.ui.settings.backup

import com.saurabhsandav.core.di.AppModule
import kotlinx.coroutines.CoroutineScope

internal class BackupSettingsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> BackupSettingsPresenter = {

        BackupSettingsPresenter(
            coroutineScope = coroutineScope,
            backupManager = appModule.backupManager,
            restoreScheduler = appModule.restoreScheduler,
        )
    }
}
