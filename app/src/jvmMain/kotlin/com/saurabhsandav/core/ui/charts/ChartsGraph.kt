package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.ui.common.UIMessagesState
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

@GraphExtension(ChartsGraph::class)
internal interface ChartsGraph {

    val presenterFactory: ChartsPresenter.Factory

    val uiMessagesState: UIMessagesState

    val loginServicesManager: LoginServicesManager

    @SingleIn(ChartsGraph::class)
    @Provides
    fun providesUiMessagesState(): UIMessagesState = UIMessagesState()

    @Provides
    fun provideStockChartsStateFactory(
        stockChartsStateFactory: StockChartsState.Factory,
        marketDataProvider: ChartsMarketDataProvider,
    ): StockChartsStateFactory = StockChartsStateFactory { coroutineScope, initialParams, loadConfig ->
        stockChartsStateFactory(
            coroutineScope = coroutineScope,
            initialParams = initialParams,
            loadConfig = loadConfig,
            marketDataProvider = marketDataProvider,
        )
    }

    @GraphExtension.Factory
    interface Factory {

        fun create(): ChartsGraph
    }
}

fun interface StockChartsStateFactory {

    fun create(
        coroutineScope: CoroutineScope,
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
    ): StockChartsState
}
