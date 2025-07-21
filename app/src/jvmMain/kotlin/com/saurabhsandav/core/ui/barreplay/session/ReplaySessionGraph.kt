package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.trading.barreplay.BarReplay
import com.saurabhsandav.trading.barreplay.CandleUpdateType
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

@GraphExtension(ReplaySessionGraph::class)
internal interface ReplaySessionGraph {

    val presenterProvider: Provider<ReplaySessionPresenter>

    @SingleIn(ReplaySessionGraph::class)
    @Provides
    fun provideBarReplay(replayParams: ReplayParams): BarReplay = BarReplay(
        timeframe = replayParams.baseTimeframe,
        from = replayParams.replayFrom,
        candleUpdateType = if (replayParams.replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )

    @Provides
    fun provideProfileId(replayParams: ReplayParams): ProfileId? = replayParams.profileId

    @Provides
    fun provideStockChartsStateFactory(
        stockChartsStateFactory: StockChartsState.Factory,
        marketDataProvider: ReplayChartsMarketDataProvider,
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

        fun create(
            @Provides coroutineScope: CoroutineScope,
            @Provides replayParams: ReplayParams,
        ): ReplaySessionGraph
    }
}

fun interface StockChartsStateFactory {

    operator fun invoke(
        coroutineScope: CoroutineScope,
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
    ): StockChartsState
}
