package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.trading.core.SessionChecker

internal class ChartsMarketDataProvider(
    private val markersProvider: ChartMarkersProvider,
    private val candleRepo: CandleRepository,
) : MarketDataProvider {

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
