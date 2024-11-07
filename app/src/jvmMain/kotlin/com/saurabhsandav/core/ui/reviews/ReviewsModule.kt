package com.saurabhsandav.core.ui.reviews

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
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
