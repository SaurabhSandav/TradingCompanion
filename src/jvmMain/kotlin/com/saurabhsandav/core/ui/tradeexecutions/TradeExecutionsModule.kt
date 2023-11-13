package com.saurabhsandav.core.ui.tradeexecutions

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        TradeExecutionsPresenter(
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
