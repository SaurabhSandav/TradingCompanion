package com.saurabhsandav.core.ui.trades

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class TradesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        TradesPresenter(
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
