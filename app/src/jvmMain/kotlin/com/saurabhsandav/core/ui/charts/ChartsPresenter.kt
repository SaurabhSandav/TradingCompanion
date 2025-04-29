package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.UriHandler
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.MarkTrades
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.OpenChart
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.common.UIMessageDuration
import com.saurabhsandav.core.ui.common.UIMessageResult
import com.saurabhsandav.core.ui.common.UIMessagesState
import com.saurabhsandav.core.ui.loginservice.LoginServicesManager
import com.saurabhsandav.core.ui.loginservice.ResultHandle
import com.saurabhsandav.core.ui.loginservice.fyers.FyersLoginService
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.data.LoadConfig
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.NIFTY500
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.fyersapi.FyersApi
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
    private val uriHandler: UriHandler,
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

    init {
        loginFlowLauncher()
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ChartsState(
            chartsState = produceState<StockChartsState?>(null) { value = chartsState.await() }.value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ChartsEvent) {

        when (event) {
            is OpenChart -> onOpenChart(event.ticker, event.start, event.end)
            is MarkTrades -> onMarkTrades(event.tradeIds)
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

        val chartsState = chartsState.await()

        // Get existing chart for params or create a new one
        val chart = run {

            if (chartsState.syncPrefs.value.dateRange) {
                val existingChart = chartsState.charts.find { it.params == tickerDTParams }
                if (existingChart != null) return@run existingChart
            }

            chartsState.newChart(tickerDTParams, null)
        }

        chartsState.bringToFront(chart)

        coroutineScope.launch { chart.navigateTo(start, end) }
    }

    private fun onMarkTrades(tradeIds: List<ProfileTradeId>) {
        markersProvider.setMarkedTrades(tradeIds)
    }

    private fun loginFlowLauncher() = coroutineScope.launchUnit {

        val isLoggedIn = candleRepo.isLoggedIn().first()

        if (isLoggedIn) return@launchUnit

        // If not logged in, show login confirmation
        val result = uiMessagesState.showMessage(
            message = "Login required to fetch candle data",
            actionLabel = "LOGIN",
            withDismissAction = true,
            duration = UIMessageDuration.Indefinite,
        )

        if (result == UIMessageResult.ActionPerformed) {

            loginServicesManager.addService(
                serviceBuilder = FyersLoginService.Builder(
                    appDispatchers = appDispatchers,
                    fyersApi = fyersApi,
                    appPrefs = appPrefs,
                    uriHandler = uriHandler,
                ),
                resultHandle = ResultHandle(
                    onFailure = { message ->
                        coroutineScope.launch {
                            uiMessagesState.showMessage(
                                message = message ?: "Unknown Error",
                                duration = UIMessageDuration.Long,
                            )
                        }
                    },
                ),
            )
        }
    }
}
