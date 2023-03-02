package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeTicker
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeTimeframe
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.ChartInfo
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialTicker = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val pagedChartArrangement = ChartArrangement.paged()
    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var selectedChartSession: ChartSession? = null
    private val chartSessions = mutableListOf<ChartSession>()
    private var chartInfo by mutableStateOf(ChartInfo())
    private val tabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is ChangeTicker -> onChangeTicker(event.newTicker)
                is ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ChartsState(
            tabsState = tabsState,
            chartPageState = chartPageState,
            chartInfo = chartInfo,
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    private fun newChart(
        tabId: Int,
        updateTitle: (String) -> Unit,
    ) {

        // Add new chart
        val actualChart = pagedChartArrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        val stockChart = StockChart(
            appModule = appModule,
            actualChart = actualChart,
            onLegendUpdate = { pagedChartArrangement.setLegend(actualChart, it) },
            onTitleUpdate = updateTitle,
        )

        // Create new chart session
        val chartSession = ChartSession(
            tabId = tabId,
            stockChart = stockChart,
            getCandles = ::getCandles,
        )

        // Connect chart to web page
        chartPageState.connect(
            chart = chartSession.stockChart.actualChart,
            syncConfig = ChartPageState.SyncConfig(
                isChartFocused = { selectedChartSession == chartSession },
                syncChartWith = { chart ->
                    val filterChartSession = chartSessions.find { it.stockChart.actualChart == chart }!!
                    val filterChartParams = filterChartSession.stockChart.currentParams ?: return@SyncConfig false
                    val chartParams = chartSession.stockChart.currentParams ?: return@SyncConfig false
                    // Sync charts with same timeframes
                    filterChartParams.timeframe == chartParams.timeframe
                },
            )
        )

        // Cache newly created chart session
        chartSessions += chartSession

        // Set chart params
        // If selected chartParams is null, this is the first chart. Initialize it with initial params.
        chartSession.newParams(
            ticker = selectedChartSession?.stockChart?.currentParams?.ticker ?: initialTicker,
            timeframe = selectedChartSession?.stockChart?.currentParams?.timeframe ?: initialTimeframe,
        )
    }

    private fun closeChart(tabId: Int) {

        // Find chart session associated with tab
        val chartSession = chartSessions.find { it.tabId == tabId }.let(::requireNotNull)

        // Remove chart session from cache
        chartSessions.remove(chartSession)

        // Remove chart page
        pagedChartArrangement.removeChart(chartSession.stockChart.actualChart)

        // Disconnect chart from web page
        chartPageState.disconnect(chartSession.stockChart.actualChart)

        // Destroy chart
        chartSession.stockChart.destroy()
    }

    private fun selectChart(tabId: Int) {

        // Find chart session associated with tab
        val chartSession = chartSessions.find { it.tabId == tabId }.let(::requireNotNull)

        // Update current chart session
        selectedChartSession = chartSession

        // Display newly selected chart info
        chartInfo = ChartInfo(chartSession.stockChart)

        // Show selected chart
        pagedChartArrangement.showChart(chartSession.stockChart.actualChart)
    }

    private fun onChangeTicker(ticker: String) {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)

        // New chart params
        chartSession.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(timeframe: Timeframe) {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)

        // New chart params
        chartSession.newParams(timeframe = timeframe)
    }

    private suspend fun getCandles(
        ticker: String,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
        retryOnLogin: Boolean = true,
    ): List<Candle> {

        val candlesResult = candleRepo.getCandles(
            ticker = ticker,
            timeframe = timeframe,
            from = range.start,
            to = range.endInclusive,
        )

        return when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> {

                    if (retryOnLogin) {
                        val loginSuccessful = onCandleDataLogin()
                        if (loginSuccessful) return getCandles(ticker, timeframe, range, false)
                    }

                    error("AuthError")
                }

                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private suspend fun onCandleDataLogin(): Boolean = suspendCoroutine {
        errors += UIErrorMessage(
            message = "Please login",
            actionLabel = "Login",
            onActionClick = {
                fyersLoginWindowState = FyersLoginWindow.Open(
                    FyersLoginState(
                        fyersApi = fyersApi,
                        appPrefs = appPrefs,
                        onCloseRequest = {
                            fyersLoginWindowState = FyersLoginWindow.Closed
                        },
                        onLoginSuccess = { it.resume(true) },
                        onLoginFailure = { message ->
                            errors += UIErrorMessage(message ?: "Unknown Error") { errors -= it }
                            it.resume(false)
                        },
                    )
                )
            },
            withDismissAction = true,
            duration = UIErrorMessage.Duration.Indefinite,
            onNotified = { errors -= it },
        )
    }
}
