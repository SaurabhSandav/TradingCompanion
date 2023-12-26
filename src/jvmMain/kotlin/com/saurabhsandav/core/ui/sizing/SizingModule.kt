package com.saurabhsandav.core.ui.sizing

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class SizingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter = {

        SizingPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            account = appModule.account,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
