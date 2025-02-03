package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.CoroutineScope

internal class TradeModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: (
        ProfileTradeId,
        onCloseRequest: () -> Unit,
    ) -> TradePresenter = { profileTradeId, onCloseRequest ->

        TradePresenter(
            profileTradeId = profileTradeId,
            onCloseRequest = onCloseRequest,
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            excursionsGenerator = appModule.tradeExcursionsGenerator,
        )
    }
}
