package com.saurabhsandav.core.ui.settings

import com.saurabhsandav.core.di.AppModule
import kotlinx.coroutines.CoroutineScope

internal class SettingsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> SettingsPresenter = {

        SettingsPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            backupManager = appModule.backupManager,
            restoreScheduler = appModule.restoreScheduler,
        )
    }
}
