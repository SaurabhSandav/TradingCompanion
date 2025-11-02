package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.trading.getSymbolOrError
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.trading.candledata.CandleRepository
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@SingleIn(ChartsGraph::class)
@Inject
internal class ChartsMarketDataProvider(
    private val markersProvider: ChartMarkersProvider,
    private val candleRepo: CandleRepository,
    private val symbolsProvider: SymbolsProvider,
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

    override fun getSymbolTitle(symbolId: SymbolId): Flow<String> {
        return symbolsProvider.getSymbolOrError(FinvasiaBroker.Id, symbolId).map { symbol ->
            "${symbol.ticker} - ${symbol.exchange}"
        }
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker
}
