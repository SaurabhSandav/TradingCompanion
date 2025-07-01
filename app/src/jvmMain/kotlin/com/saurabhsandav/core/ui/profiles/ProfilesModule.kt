package com.saurabhsandav.core.ui.profiles

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class ProfilesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenterFactory: ProfilesPresenter.Factory = object : ProfilesPresenter.Factory {

        override fun build(
            customSelectionMode: Boolean,
            trainingOnly: Boolean,
            selectedProfileId: ProfileId?,
            onProfileSelected: ((ProfileId?) -> Unit)?,
        ) = ProfilesPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
            customSelectionMode = customSelectionMode,
            trainingOnly = trainingOnly,
            selectedProfileId = selectedProfileId,
            onProfileSelected = onProfileSelected,
        )
    }
}
