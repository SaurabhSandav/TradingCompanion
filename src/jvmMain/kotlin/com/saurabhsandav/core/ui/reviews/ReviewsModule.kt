package com.saurabhsandav.core.ui.reviews

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class ReviewsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        ReviewsPresenter(
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            appPrefs = appModule.appPrefs,
        )
    }
}
