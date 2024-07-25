package com.saurabhsandav.core.ui.trades

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TradesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    val profileId: ProfileId,
) {

    val presenter = {

        TradesPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
