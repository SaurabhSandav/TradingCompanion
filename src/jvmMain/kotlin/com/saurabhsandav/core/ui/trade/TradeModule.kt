package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import kotlinx.coroutines.CoroutineScope

internal class TradeModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    tradeContentLauncher: TradeContentLauncher,
) {

    val presenter = { profileTradeId: ProfileTradeId ->

        TradePresenter(
            profileTradeId = profileTradeId,
            coroutineScope = coroutineScope,
            tradeContentLauncher = tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}
