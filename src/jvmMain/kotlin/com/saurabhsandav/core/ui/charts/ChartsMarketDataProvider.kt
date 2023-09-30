package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ChartsMarketDataProvider(
    private val markersProvider: ChartMarkersProvider,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = appModule.candleRepo,
) : MarketDataProvider {

    override fun symbols(): StateFlow<ImmutableList<String>> {
        return MutableStateFlow(NIFTY50)
    }

    override fun timeframes(): StateFlow<ImmutableList<Timeframe>> {
        return MutableStateFlow(Timeframe.entries.toImmutableList())
    }

    override fun buildCandleSource(params: StockChartParams): ChartsCandleSource {
        return ChartsCandleSource(
            params = params,
            candleRepo = candleRepo,
            getTradeMarkers = { candleSeries ->
                markersProvider.getTradeMarkers(params.ticker, candleSeries)
            },
            getTradeExecutionMarkers = { candleSeries ->
                markersProvider.getTradeExecutionMarkers(params.ticker, candleSeries)
            },
        )
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.ticker != "NIFTY50"
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker
}
