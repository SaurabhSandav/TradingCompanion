package com.saurabhsandav.core.ui.charts.tradereview

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Instant

internal class TradeReviewModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            initialMarkedTrades: List<ProfileTradeId>,
            onOpenChart: (
                ticker: String,
                start: Instant,
                end: Instant?,
            ) -> Unit,
            onMarkTrades: (tradeIds: List<ProfileTradeId>) -> Unit,
        ->

        TradeReviewPresenter(
            coroutineScope = coroutineScope,
            initialMarkedTrades = initialMarkedTrades,
            onOpenChart = onOpenChart,
            onMarkTrades = onMarkTrades,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            appPrefs = appModule.appPrefs,
        )
    }
}
