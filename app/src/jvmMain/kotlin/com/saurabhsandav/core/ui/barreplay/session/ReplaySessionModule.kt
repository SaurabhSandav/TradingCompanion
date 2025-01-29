package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.coroutines.CoroutineScope

internal class ReplaySessionModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    replayParams: BarReplayState.ReplayParams,
) {

    private val barReplay = BarReplay(
        timeframe = replayParams.baseTimeframe,
        from = replayParams.replayFrom,
        candleUpdateType = if (replayParams.replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )

    private val replaySeriesCache = ReplaySeriesCache(
        replayParams = replayParams,
        barReplay = barReplay,
        candleRepo = appModule.candleRepo,
    )

    private val replayOrdersManager = ReplayOrdersManager(
        coroutineScope = coroutineScope,
        profileId = replayParams.profileId,
        replaySeriesCache = replaySeriesCache,
        tradingProfiles = appModule.tradingProfiles,
    )

    private val marketDataProvider = ReplayChartsMarketDataProvider(
        appDispatchers = appModule.appDispatchers,
        coroutineScope = coroutineScope,
        profileId = replayParams.profileId,
        replaySeriesCache = replaySeriesCache,
        tradingProfiles = appModule.tradingProfiles,
    )

    val presenter: () -> ReplaySessionPresenter = {

        ReplaySessionPresenter(
            coroutineScope = coroutineScope,
            replayParams = replayParams,
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
            barReplay = barReplay,
            replayOrdersManager = replayOrdersManager,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

fun interface StockChartsStateFactory {

    operator fun invoke(
        initialParams: StockChartParams,
        loadConfig: LoadConfig,
    ): StockChartsState
}
