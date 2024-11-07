package com.saurabhsandav.core.ui.landing

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class LandingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> LandingPresenter = {

        LandingPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
