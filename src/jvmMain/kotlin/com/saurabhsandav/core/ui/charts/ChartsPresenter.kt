package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.fyers_api.FyersApi
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
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val marketDataProvider = ChartsMarketDataProvider(appModule = appModule)
    private val chartsState = StockChartsState(
        initialParams = StockChartParams(initialTicker, initialTimeframe),
        marketDataProvider = marketDataProvider,
        appModule = appModule,
    )
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
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

    init {
        loginFlowLauncher()
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    fun addMarkersProvider(provider: ChartMarkersProvider) {
        marketDataProvider.addMarkersProvider(provider)
    }

    fun removeMarkersProvider(provider: ChartMarkersProvider) {
        marketDataProvider.removeMarkersProvider(provider)
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

            val stockChart = chartsState.charts.firstOrNull { it.currentParams == params }

            if (stockChart == null) {

                // Chart doesn't exist, create a new one
                val newStockChart = chartsState.newChart(params)

                // Bring default timeframe chart for ticker to front
                if (params == tickerDTParams) chartsState.bringToFront(newStockChart)

                coroutineScope.launch {

                    // Load data for specified interval
                    newStockChart.loadInterval(start, end).await()

                    // Navigate default timeframe chart to specified interval
                    if (params == tickerDTParams) newStockChart.navigateToInterval(start, end)
                }

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

    private fun loginFlowLauncher() {

        candleRepo.isLoggedIn().onEach { isLoggedIn ->

            // If not logged in and login window not launched
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
        }.launchIn(coroutineScope)
    }
}
