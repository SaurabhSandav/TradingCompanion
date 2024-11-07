package com.saurabhsandav.core.ui.review

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import kotlinx.coroutines.CoroutineScope

internal class ReviewModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileReviewId: ProfileReviewId,
) {

    val presenter: () -> ReviewPresenter = {

        ReviewPresenter(
            coroutineScope = coroutineScope,
            profileReviewId = profileReviewId,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
