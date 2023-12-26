package com.saurabhsandav.core.ui.tags

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TagsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter = {

        TagsPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
