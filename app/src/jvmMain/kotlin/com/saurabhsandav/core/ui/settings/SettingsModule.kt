package com.saurabhsandav.core.ui.settings

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.settings.backup.BackupSettingsModule
import kotlinx.coroutines.CoroutineScope

internal class SettingsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> SettingsPresenter = {

        SettingsPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
        )
    }

    val backupSettingsModule: (CoroutineScope) -> BackupSettingsModule = { coroutineScope ->
        BackupSettingsModule(appModule, coroutineScope)
    }
}
