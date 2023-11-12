package com.saurabhsandav.core.ui.profiles

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class ProfilesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = ProfilesPresenter.Factory {
            customSelectionMode: Boolean,
            trainingOnly: Boolean,
        ->

        ProfilesPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
            customSelectionMode = customSelectionMode,
            trainingOnly = trainingOnly,
        )
    }
}
