package com.saurabhsandav.core.ui.stockchart

import kotlinx.coroutines.CoroutineScope

fun interface StockChartsStateFactory {

    operator fun invoke(
        coroutineScope: CoroutineScope,
        initialParams: StockChartParams,
        marketDataProvider: MarketDataProvider,
    ): StockChartsState
}
