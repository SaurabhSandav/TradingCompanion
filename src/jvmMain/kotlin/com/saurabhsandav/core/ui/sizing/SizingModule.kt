package com.saurabhsandav.core.ui.sizing

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class SizingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        SizingPresenter(
            coroutineScope = coroutineScope,
            account = appModule.account,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
