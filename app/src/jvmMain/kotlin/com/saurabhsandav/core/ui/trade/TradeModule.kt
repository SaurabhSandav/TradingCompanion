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
        onProfileStoppedExisting: () -> Unit,
    ) -> TradePresenter = { profileTradeId, onProfileStoppedExisting ->

        TradePresenter(
            profileTradeId = profileTradeId,
            onProfileStoppedExisting = onProfileStoppedExisting,
            coroutineScope = coroutineScope,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            excursionsGenerator = appModule.tradeExcursionsGenerator,
        )
    }
}
