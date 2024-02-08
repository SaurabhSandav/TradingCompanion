package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.LoadConfig
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

    private val replayOrdersManager = ReplayOrdersManager(
        coroutineScope = coroutineScope,
        replayParams = replayParams,
        barReplay = barReplay,
        appPrefs = appModule.appPrefs,
        tradingProfiles = appModule.tradingProfiles,
        candleRepo = appModule.candleRepo,
    )

    private val marketDataProvider = ReplayChartsMarketDataProvider(
        coroutineScope = coroutineScope,
        replayParams = replayParams,
        barReplay = barReplay,
        appPrefs = appModule.appPrefs,
        candleRepo = appModule.candleRepo,
        tradingProfiles = appModule.tradingProfiles,
    )

    val presenter = {

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
            appPrefs = appModule.appPrefs,
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
