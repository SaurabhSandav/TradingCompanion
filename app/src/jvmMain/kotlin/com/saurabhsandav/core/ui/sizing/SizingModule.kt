package com.saurabhsandav.core.ui.sizing

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class SizingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> SizingPresenter = {

        SizingPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            account = appModule.account,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
