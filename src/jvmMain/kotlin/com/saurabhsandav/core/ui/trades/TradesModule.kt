package com.saurabhsandav.core.ui.trades

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import kotlinx.coroutines.CoroutineScope

internal class TradesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    tradeContentLauncher: TradeContentLauncher,
) {

    val presenter = {

        TradesPresenter(
            coroutineScope = coroutineScope,
            tradeContentLauncher = tradeContentLauncher,
            appPrefs = appModule.appPrefs,
            candleRepo = appModule.candleRepo,
            tradingProfiles = appModule.tradingProfiles,
            loginServicesManager = appModule.loginServicesManager,
            fyersApi = appModule.fyersApi,
        )
    }
}
