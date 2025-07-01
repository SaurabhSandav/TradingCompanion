package com.saurabhsandav.core.ui.trades

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TradesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    val profileId: ProfileId,
) {

    val presenter: () -> TradesPresenter = {

        TradesPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
