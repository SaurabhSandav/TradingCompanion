package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
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
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

    private val marketDataProvider = ChartsMarketDataProvider(appModule = appModule)
    private val chartsState = coroutineScope.async {

        val defaultTimeframe = appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
            .map(Timeframe::valueOf)
            .first()

        StockChartsState(
            initialParams = StockChartParams(NIFTY50.first(), defaultTimeframe),
            marketDataProvider = marketDataProvider,
            appModule = appModule,
        )
    }
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    init {
        loginFlowLauncher()
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ChartsState(
            chartsState = produceState<StockChartsState?>(null) { value = chartsState.await() }.value,
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ChartsEvent) {

        when (event) {
            is OpenChart -> onOpenChart(event.ticker, event.start, event.end)
            CandleFetchLoginCancelled -> onCandleFetchLoginCancelled()
        }
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
    ) = coroutineScope.launchUnit {

        val defaultTimeframe = appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
            .map(Timeframe::valueOf)
            .first()

        // Default timeframe chart for ticker
        val tickerDTParams = StockChartParams(ticker, defaultTimeframe)

        val chartParams = buildList {

            if (defaultTimeframe != Timeframe.D1) {

                // Daily chart for index.
                add(StockChartParams(NIFTY50.first(), Timeframe.D1))

                // Daily chart for ticker.
                add(StockChartParams(ticker, Timeframe.D1))
            }

            // Default timeframe chart for index.
            add(StockChartParams(NIFTY50.first(), defaultTimeframe))

            // Default timeframe chart for ticker. Also bring to front.
            add(tickerDTParams)
        }

        val chartsState = chartsState.await()

        chartParams.forEach { params ->

            // Get existing chart for params or create a new one
            val stockChart = chartsState.charts.firstOrNull { it.params == params } ?: chartsState.newChart(params)

            if (params == tickerDTParams) {

                // Bring default timeframe chart for ticker to front
                chartsState.bringToFront(stockChart)

                // Navigate default timeframe chart to specified interval
                coroutineScope.launch { stockChart.navigateTo(start, end) }
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
