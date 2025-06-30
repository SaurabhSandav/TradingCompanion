package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.trading.candledata.CandleRepository
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
                markersProvider.getTradeMarkers(params.symbolId, instantRange)
            },
            getTradeExecutionMarkers = { instantRange ->
                markersProvider.getTradeExecutionMarkers(params.symbolId, instantRange)
            },
        )
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.symbolId.value != "NIFTY50"
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker
}
