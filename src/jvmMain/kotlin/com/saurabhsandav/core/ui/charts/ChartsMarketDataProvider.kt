package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*

internal class ChartsMarketDataProvider(
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = appModule.candleRepo,
) : MarketDataProvider {

    private val chartMarkersProviders = MutableStateFlow(persistentListOf<ChartMarkersProvider>())

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
            getMarkers = { candleSeries -> getMarkers(params.ticker, candleSeries) },
        )
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.ticker != "NIFTY50"
    }

    fun addMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.add(provider)
    }

    fun removeMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.remove(provider)
    }

    private fun getMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<SeriesMarker>> {
        return chartMarkersProviders
            .map { it.map { provider -> provider.provideMarkers(ticker, candleSeries) } }
            .flatMapLatest { flows -> combine(flows) { it.toList().flatten() } }
    }
}
