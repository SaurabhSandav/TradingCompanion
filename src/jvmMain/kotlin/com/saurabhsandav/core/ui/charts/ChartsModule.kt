package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.stockchart.LoadConfig
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.coroutines.CoroutineScope

internal class ChartsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    private val markersProvider = ChartMarkersProvider(
        tradingProfiles = appModule.tradingProfiles,
    )

    private val marketDataProvider = ChartsMarketDataProvider(
        markersProvider = markersProvider,
        candleRepo = appModule.candleRepo,
    )

    val presenter = {

        ChartsPresenter(
            coroutineScope = coroutineScope,
            stockChartsStateFactory = {
                    initialParams: StockChartParams,
                    loadConfig: LoadConfig,
                ->

                appModule.stockChartsState(
                    coroutineScope = coroutineScope,
                    initialParams = initialParams,
                    loadConfig = loadConfig,
                    marketDataProvider = marketDataProvider,
                )
            },
            markersProvider = markersProvider,
            appPrefs = appModule.appPrefs,
            loginServicesManager = appModule.loginServicesManager,
            fyersApi = appModule.fyersApi,
            candleRepo = appModule.candleRepo,
        )
    }
}

fun interface StockChartsStateFactory {

    operator fun invoke(
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
    ): StockChartsState
}
