package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.CoroutineScope

internal class TradeModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = { profileTradeId: ProfileTradeId ->

        TradePresenter(
            profileTradeId = profileTradeId,
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            excursionsGenerator = appModule.tradeExcursionsGenerator,
        )
    }
}
