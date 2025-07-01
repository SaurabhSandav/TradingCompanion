package com.saurabhsandav.core.ui.reviews

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class ReviewsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> ReviewsPresenter = {

        ReviewsPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
