package com.saurabhsandav.core.ui.charts.tradereview

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Instant

internal class TradeReviewModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            onOpenChart: (
                ticker: String,
                start: Instant,
                end: Instant?,
            ) -> Unit,
            markersProvider: ChartMarkersProvider,
        ->

        TradeReviewPresenter(
            coroutineScope = coroutineScope,
            onOpenChart = onOpenChart,
            markersProvider = markersProvider,
            tradeContentLauncher = appModule.tradeContentLauncher,
            tradingProfiles = appModule.tradingProfiles,
            appPrefs = appModule.appPrefs,
        )
    }
}
