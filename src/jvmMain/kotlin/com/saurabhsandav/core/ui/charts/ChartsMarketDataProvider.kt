package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

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
            getCandles = { range -> getCandles(params, range) },
            getMarkers = { candleSeries -> getMarkers(params.ticker, candleSeries) },
        )
    }

    fun addMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.add(provider)
    }

    fun removeMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.remove(provider)
    }

    private suspend fun getCandles(
        params: StockChartParams,
        range: ClosedRange<Instant>,
    ): List<Candle> {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { it }

        // Retry until request successful
        val candlesResult = retryIOResult(
            initialDelay = 1000,
            maxDelay = 10000,
        ) {

            candleRepo.getCandles(
                ticker = params.ticker,
                timeframe = params.timeframe,
                from = range.start,
                to = range.endInclusive,
            )
        }

        return when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
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
