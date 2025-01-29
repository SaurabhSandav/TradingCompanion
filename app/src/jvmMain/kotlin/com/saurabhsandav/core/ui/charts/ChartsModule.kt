package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.common.UIMessagesState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.coroutines.CoroutineScope

internal class ChartsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val uiMessagesState = UIMessagesState()

    private val markersProvider = ChartMarkersProvider(
        appDispatchers = appModule.appDispatchers,
        tradingProfiles = appModule.tradingProfiles,
    )

    private val marketDataProvider = ChartsMarketDataProvider(
        markersProvider = markersProvider,
        candleRepo = appModule.candleRepo,
    )

    val presenter: () -> ChartsPresenter = {

        ChartsPresenter(
            appDispatchers = appModule.appDispatchers,
            coroutineScope = coroutineScope,
            uiMessagesState = uiMessagesState,
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
