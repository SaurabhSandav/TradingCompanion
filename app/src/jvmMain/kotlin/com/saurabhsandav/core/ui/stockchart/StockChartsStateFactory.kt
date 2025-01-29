package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import kotlinx.coroutines.CoroutineScope

fun interface StockChartsStateFactory {

    operator fun invoke(
        coroutineScope: CoroutineScope,
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
        marketDataProvider: MarketDataProvider,
    ): StockChartsState
}
