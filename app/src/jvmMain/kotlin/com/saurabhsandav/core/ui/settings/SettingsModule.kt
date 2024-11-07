package com.saurabhsandav.core.ui.settings

import com.saurabhsandav.core.AppModule
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
}
