package com.saurabhsandav.core.ui.tradeexecutions

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> TradeExecutionsPresenter = {

        TradeExecutionsPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
