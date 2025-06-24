package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.core.SessionChecker
import com.saurabhsandav.core.trading.core.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.core.utils.NIFTY500
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ChartsMarketDataProvider(
    private val markersProvider: ChartMarkersProvider,
    private val candleRepo: CandleRepository,
) : MarketDataProvider {

    override fun symbols(): StateFlow<List<String>> {
        return MutableStateFlow(NIFTY500)
    }

    override fun timeframes(): StateFlow<List<Timeframe>> {
        return MutableStateFlow(Timeframe.entries.toList())
    }

    override fun buildCandleSource(params: StockChartParams): ChartsCandleSource {
        return ChartsCandleSource(
            params = params,
            candleRepo = candleRepo,
            getTradeMarkers = { instantRange ->
                markersProvider.getTradeMarkers(params.ticker, instantRange)
            },
            getTradeExecutionMarkers = { instantRange ->
                markersProvider.getTradeExecutionMarkers(params.ticker, instantRange)
            },
        )
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.ticker != "NIFTY50"
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker
}
