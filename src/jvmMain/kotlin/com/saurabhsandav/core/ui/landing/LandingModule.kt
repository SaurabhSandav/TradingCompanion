package com.saurabhsandav.core.ui.landing

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class LandingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        LandingPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
