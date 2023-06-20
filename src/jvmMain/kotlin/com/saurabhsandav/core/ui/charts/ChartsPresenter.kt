package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.CandleFetchLoginCancelled
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.OpenChart
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Stable
internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
    private val candleRepo: CandleRepository = appModule.candleRepo,
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialTicker = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val stockCharts = mutableListOf<StockChart>()
    private val candleSources = mutableMapOf<StockChartParams, ChartsCandleSource>()
    private val queuedChartInitializers = mutableListOf<StockChart.() -> Unit>()
    private val chartsState = StockChartsState(
        onNewChart = ::onNewChart,
        onCloseChart = ::onCloseChart,
        onChangeTicker = ::onChangeTicker,
        onChangeTimeframe = ::onChangeTimeframe,
        appModule = appModule,
    )
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val chartMarkersProviders = MutableStateFlow(persistentListOf<ChartMarkersProvider>())
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is OpenChart -> onOpenChart(event.ticker, event.start, event.end)
                CandleFetchLoginCancelled -> onCandleFetchLoginCancelled()
            }
        }

        return@launchMolecule ChartsState(
            chartsState = chartsState,
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    fun addMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.add(provider)
    }

    fun removeMarkersProvider(provider: ChartMarkersProvider) {

        chartMarkersProviders.value = chartMarkersProviders.value.remove(provider)
    }

    private fun onNewChart(
        newStockChart: StockChart,
        prevStockChart: StockChart?,
    ) {

        // Cache StockChart
        stockCharts += newStockChart

        // Set Data
        val queuedChartInitializer = queuedChartInitializers.removeFirstOrNull()

        when {
            queuedChartInitializer != null -> queuedChartInitializer.invoke(newStockChart)
            else -> newStockChart.newParams(
                ticker = prevStockChart?.currentParams?.ticker ?: initialTicker,
                timeframe = prevStockChart?.currentParams?.timeframe ?: initialTimeframe,
            )
        }
    }

    private fun onCloseChart(stockChart: StockChart) {

        // Remove chart session from cache
        stockCharts.remove(stockChart)

        // Destroy chart
        stockChart.destroy()

        // Remove unused ChartCandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun onChangeTicker(stockChart: StockChart, ticker: String) {

        // New chart params
        stockChart.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(stockChart: StockChart, timeframe: Timeframe) {

        // New chart params
        stockChart.newParams(timeframe = timeframe)
    }

    private fun onOpenChart(
        ticker: String,
        start: Instant,
        end: Instant?,
    ) {

        // Default timeframe (Currently hardcoded to M5) chart for ticker
        val tickerDTParams = StockChartParams(ticker, Timeframe.M5)

        val chartParams = listOf(
            // Daily chart for index.
            StockChartParams(NIFTY50.first(), Timeframe.D1),
            // Default timeframe (Currently hardcoded to M5) chart for index.
            StockChartParams(NIFTY50.first(), Timeframe.M5),
            // Daily chart for ticker.
            StockChartParams(ticker, Timeframe.D1),
            // Default timeframe (Currently hardcoded to M5) chart for ticker. Also bring to front.
            tickerDTParams,
        )

        chartParams.forEach { params ->

            val stockChart = stockCharts.firstOrNull { it.currentParams == params }

            if (stockChart == null) {

                queuedChartInitializers.add {

                    coroutineScope.launch {

                        // Previously this line and the next line (val loadFinish...) came before the launch scope.
                        // After StockChart refactor, an error occurs because new chart callback is received
                        // before chart is added to window. Moving this line inside launch causes this line to be
                        // executed once all other non suspend code (including adding chart to page) on this thread
                        // has finished executing. This is temporary solution (TODO).
                        //
                        // Bring default timeframe chart for ticker to front
                        if (params == tickerDTParams) chartsState.bringToFront(this@add)

                        // Set Data. setCandleSource() needs to be called synchronously when ticker matches index chart.
                        // In such a case, an instance the index chart may open twice.
                        val loadFinish = newParams(params.ticker, params.timeframe)

                        // Wait for data to be loaded
                        loadFinish.await()

                        // Load data for specified interval
                        loadInterval(start, end).await()

                        // Navigate default timeframe chart to specified interval
                        if (params == tickerDTParams) navigateToInterval(start, end)
                    }
                }

                // Open new tab
                chartsState.openNewTab()

            } else {

                // Bring default timeframe chart for ticker to front
                if (params == tickerDTParams) chartsState.bringToFront(stockChart)

                coroutineScope.launch {

                    // Load data for specified interval
                    stockChart.loadInterval(start, end).await()

                    // Navigate default timeframe chart to specified interval
                    if (params == tickerDTParams) stockChart.navigateToInterval(start, end)
                }
            }
        }
    }

    private fun onCandleFetchLoginCancelled() {
        fyersLoginWindowState = FyersLoginWindow.Closed
    }

    private fun StockChart.newParams(
        ticker: String? = currentParams?.ticker,
        timeframe: Timeframe? = currentParams?.timeframe,
    ): CompletableDeferred<Unit> {

        check(ticker != null && timeframe != null) {
            "Ticker ($ticker) and/or Timeframe ($timeframe) cannot be null"
        }

        val params = StockChartParams(ticker, timeframe)

        val candleSource = candleSources.getOrPut(params) {
            ChartsCandleSource(
                params = params,
                getCandles = { range -> getCandles(params, range) },
                getMarkers = { candleSeries -> getMarkers(ticker, candleSeries) },
            )
        }

        // Set ChartCandleSource on StockChart
        val deferred = setCandleSource(candleSource)

        // Remove unused ChartCandleSources from cache
        releaseUnusedCandleSources()

        return deferred
    }

    private fun releaseUnusedCandleSources() {

        // CandleSources currently in use
        val usedCandleSources = stockCharts.mapNotNull { stockChart -> stockChart.source }

        // CandleSources not in use
        val unusedCandleSources = candleSources.filter { it.value !in usedCandleSources }

        // Remove unused CandleSource from cache
        unusedCandleSources.forEach { candleSources.remove(it.key) }
    }

    private suspend fun getCandles(
        params: StockChartParams,
        range: ClosedRange<Instant>,
    ): List<Candle> {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { isLoggedIn ->

            if (!isLoggedIn && fyersLoginWindowState !is FyersLoginWindow.Open) {

                fyersLoginWindowState = FyersLoginWindow.Open(
                    FyersLoginState(
                        fyersApi = fyersApi,
                        appPrefs = appPrefs,
                        onCloseRequest = {
                            fyersLoginWindowState = FyersLoginWindow.Closed
                        },
                        onLoginSuccess = { },
                        onLoginFailure = { message ->
                            errors += UIErrorMessage(message ?: "Unknown Error") { errors -= it }
                        },
                    )
                )
            }

            isLoggedIn
        }

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
