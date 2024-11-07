package com.saurabhsandav.core.ui.tradereview

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.charts.ChartsHandle
import kotlinx.coroutines.CoroutineScope

internal class TradeReviewModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: (ChartsHandle) -> TradeReviewPresenter = { chartsHandle ->

        TradeReviewPresenter(
            coroutineScope = coroutineScope,
            chartsHandle = chartsHandle,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            appPrefs = appModule.appPrefs,
        )
    }
}
