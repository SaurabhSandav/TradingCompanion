package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import kotlinx.coroutines.CoroutineScope

internal class ChartsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val markersProvider = ChartMarkersProvider(
        tradingProfiles = appModule.tradingProfiles,
    )

    private val marketDataProvider = ChartsMarketDataProvider(
        markersProvider = markersProvider,
        candleRepo = appModule.candleRepo,
    )

    val presenter = {

        ChartsPresenter(
            coroutineScope = coroutineScope,
            stockChartsStateFactory = { initialParams: StockChartParams ->
                appModule.stockChartsState(coroutineScope, initialParams, marketDataProvider)
            },
            appPrefs = appModule.appPrefs,
            loginServicesManager = appModule.loginServicesManager,
            fyersApi = appModule.fyersApi,
            candleRepo = appModule.candleRepo,
        )
    }
}
