package com.saurabhsandav.core.ui.tags.screen

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TagsScreenModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    val profileId: ProfileId,
) {

    val presenter: () -> TagsScreenPresenter = {

        TagsScreenPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
