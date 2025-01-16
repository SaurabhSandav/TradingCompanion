package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.*
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.common.UIMessageDuration
import com.saurabhsandav.core.ui.common.UIMessagesState
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.loginservice.ResultHandle
import com.saurabhsandav.core.ui.loginservice.impl.FyersLoginService
import com.saurabhsandav.core.ui.stockchart.LoadConfig
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.utils.*
import com.saurabhsandav.fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class ChartsPresenter(
    private val appDispatchers: AppDispatchers,
    private val coroutineScope: CoroutineScope,
    private val uiMessagesState: UIMessagesState,
    stockChartsStateFactory: StockChartsStateFactory,
    private val markersProvider: ChartMarkersProvider,
    private val appPrefs: FlowSettings,
    private val loginServicesManager: LoginServicesManager,
    private val fyersApi: FyersApi,
    private val candleRepo: CandleRepository,
) {

    private val chartsState = coroutineScope.async {

        val defaultTimeframe = appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
            .map(Timeframe::valueOf)
            .first()

        stockChartsStateFactory(
            initialParams = StockChartParams(NIFTY500.first(), defaultTimeframe),
            loadConfig = LoadConfig(initialLoadBefore = { Clock.System.now() }),
        )
    }
    private var showCandleDataLoginConfirmation by mutableStateOf(false)

    init {
        loginFlowLauncher()
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ChartsState(
            chartsState = produceState<StockChartsState?>(null) { value = chartsState.await() }.value,
            showCandleDataLoginConfirmation = showCandleDataLoginConfirmation,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ChartsEvent) {

        when (event) {
            is OpenChart -> onOpenChart(event.ticker, event.start, event.end)
            is MarkTrades -> onMarkTrades(event.tradeIds)
            CandleDataLoginConfirmed -> onCandleDataLoginConfirmed()
            CandleDataLoginDeclined -> onCandleDataLoginDeclined()
        }
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
                add(StockChartParams(NIFTY500.first(), Timeframe.D1))

                // Daily chart for ticker.
                add(StockChartParams(ticker, Timeframe.D1))
            }

            // Default timeframe chart for index.
            add(StockChartParams(NIFTY500.first(), defaultTimeframe))

            // Default timeframe chart for ticker. Also bring to front.
            add(tickerDTParams)
        }

        val chartsState = chartsState.await()

        chartParams.forEach { params ->

            // Get existing chart for params or create a new one
            val chartExists = chartsState.charts.any { it.params == params }

            if (!chartExists) chartsState.newChart(params)
        }

        // Navigate to specified interval, 1 ticker chart for every timeframe. Rest will automatically sync.
        chartsState.charts
            .filter { stockChart -> stockChart.params.ticker == ticker }
            .groupBy { it.params.timeframe }
            .mapValues { it.value.first() }
            .values
            .forEach { stockChart ->

                // Bring default timeframe chart for ticker to front
                if (stockChart.params == tickerDTParams) chartsState.bringToFront(stockChart)

                coroutineScope.launch { stockChart.navigateTo(start, end) }
            }
    }

    private fun onMarkTrades(tradeIds: List<ProfileTradeId>) {
        markersProvider.setMarkedTrades(tradeIds)
    }

    private fun onCandleDataLoginConfirmed() {

        showCandleDataLoginConfirmation = false

        loginServicesManager.addService(
            serviceBuilder = FyersLoginService.Builder(
                appDispatchers = appDispatchers,
                fyersApi = fyersApi,
                appPrefs = appPrefs,
            ),
            resultHandle = ResultHandle(
                onFailure = { message ->
                    coroutineScope.launch {
                        uiMessagesState.showMessage(
                            message = message ?: "Unknown Error",
                            duration = UIMessageDuration.Long,
                        )
                    }
                }
            ),
        )
    }

    private fun onCandleDataLoginDeclined() {
        showCandleDataLoginConfirmation = false
    }

    private fun loginFlowLauncher() = coroutineScope.launchUnit {

        val isLoggedIn = candleRepo.isLoggedIn().first()

        // If not logged in, show login confirmation
        if (!isLoggedIn) showCandleDataLoginConfirmation = true
    }
}
