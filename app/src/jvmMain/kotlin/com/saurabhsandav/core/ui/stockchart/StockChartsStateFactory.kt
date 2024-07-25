package com.saurabhsandav.core.ui.stockchart

import kotlinx.coroutines.CoroutineScope

fun interface StockChartsStateFactory {

    operator fun invoke(
        coroutineScope: CoroutineScope,
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
        marketDataProvider: MarketDataProvider,
    ): StockChartsState
}
