package com.saurabhsandav.core.ui.profiles

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class ProfilesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenterFactory = object : ProfilesPresenter.Factory {

        override fun build(
            customSelectionMode: Boolean,
            trainingOnly: Boolean,
            initialSelectedProfileId: ProfileId?,
            onProfileSelected: ((ProfileId?) -> Unit)?,
        ) = ProfilesPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
            customSelectionMode = customSelectionMode,
            trainingOnly = trainingOnly,
            initialSelectedProfileId = initialSelectedProfileId,
            onProfileSelected = onProfileSelected,
        )
    }
}
