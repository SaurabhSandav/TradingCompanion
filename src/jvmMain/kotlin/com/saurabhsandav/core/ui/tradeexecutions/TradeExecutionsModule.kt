package com.saurabhsandav.core.ui.tradeexecutions

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    tradeContentLauncher: TradeContentLauncher,
) {

    val presenter = {

        TradeExecutionsPresenter(
            coroutineScope = coroutineScope,
            tradeContentLauncher = tradeContentLauncher,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
