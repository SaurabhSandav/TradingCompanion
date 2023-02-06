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
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeSymbol
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeTimeframe
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.ChartInfo
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.days

internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialSymbol = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val pagedChartArrangement = ChartArrangement.paged()
    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var selectedChartSession: ChartSession? = null
    private val chartSessions = mutableListOf<ChartSession>()
    private val downloadIntervalDays = 90.days

    private val tabsState: StockChartTabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )
    private var chartInfo by mutableStateOf(ChartInfo(initialSymbol, initialTimeframe))
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is ChangeSymbol -> onChangeSymbol(event.newSymbol)
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

    private fun newChart(tabId: Int) = coroutineScope.launchUnit {

        // Add new chart
        val actualChart = pagedChartArrangement.newChart(
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        val stockChart = StockChart(
            appModule = appModule,
            actualChart = actualChart,
            onLegendUpdate = { pagedChartArrangement.setLegend(actualChart, it) },
        )

        // Create new chart session
        val chartSession = when (selectedChartSession) {
            // First chart, create chart session with initial params
            null -> {

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(initialSymbol, initialTimeframe))

                ChartSession(
                    tabId = tabId,
                    ticker = initialSymbol,
                    timeframe = initialTimeframe,
                    stockChart = stockChart,
                )
            }
            // Copy currently selected chart session
            else -> {

                // Currently selected chart session
                val chartSession = requireNotNull(selectedChartSession)

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(chartSession.ticker, chartSession.timeframe))

                // Create new chart session with existing params
                chartSession.copy(
                    tabId = tabId,
                    stockChart = stockChart,
                )
            }
        }

        // Connect chart to web page
        chartPageState.connect(
            chart = chartSession.stockChart.actualChart,
            syncConfig = ChartPageState.SyncConfig(
                isChartFocused = { selectedChartSession == chartSession },
                syncChartWith = { chart ->
                    val filterChartInstance = chartSessions.find { it.stockChart.actualChart == chart }!!
                    // Sync charts with same timeframes
                    filterChartInstance.timeframe == chartSession.timeframe
                },
            )
        )

        // Cache newly created chart session
        chartSessions += chartSession

        // Set candle source for chart
        chartSession.stockChart.setCandleSource(chartSession.buildCandleSource())

        // Switch to new tab/chart
        selectChart(tabId)
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
        chartInfo = ChartInfo(
            symbol = chartSession.ticker,
            timeframe = chartSession.timeframe,
        )

        // Show selected chart
        pagedChartArrangement.showChart(chartSession.stockChart.actualChart)
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)
        val chartSessionIndex = chartSessions.indexOf(chartSession)

        // New chart session
        val newChartSession = chartSession.copy(ticker = symbol)

        // Update chart session
        chartSessions[chartSessionIndex] = newChartSession

        // Replace chart candle source
        newChartSession.stockChart.setCandleSource(newChartSession.buildCandleSource())

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        tabsState.setTitle(chartSession.tabId, tabTitle(symbol, chartSession.timeframe))
    }

    private fun onChangeTimeframe(timeframe: Timeframe) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)
        val chartSessionIndex = chartSessions.indexOf(chartSession)

        // New chart session
        val newChartSession = chartSession.copy(timeframe = timeframe)

        // Update chart session
        chartSessions[chartSessionIndex] = newChartSession

        // Replace chart candle source
        newChartSession.stockChart.setCandleSource(newChartSession.buildCandleSource())

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe)

        // Update tab title
        tabsState.setTitle(chartSession.tabId, tabTitle(chartSession.ticker, timeframe))
    }

    private suspend fun ChartSession.buildCandleSource(): CandleSource {

        val mutableCandleSeries = getCandleSeries(
            symbol = ticker,
            timeframe = timeframe,
            // Range of 3 months before current time to current time
            range = run {
                val currentTime = Clock.System.now()
                currentTime.minus(downloadIntervalDays)..currentTime
            }
        )

        return CandleSource(
            candleSeries = mutableCandleSeries.asCandleSeries(),
            hasVolume = ticker != "NIFTY50",
            onLoadBefore = {

                val firstCandleInstant = mutableCandleSeries.first().openInstant

                val oldCandles = getCandleSeries(
                    symbol = ticker,
                    timeframe = timeframe,
                    range = firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant
                )

                val areCandlesAvailable = oldCandles.isNotEmpty()

                if (oldCandles.isNotEmpty()) {
                    mutableCandleSeries.prependCandles(oldCandles)
                }

                areCandlesAvailable
            }
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
        retryOnLogin: Boolean = true,
    ): MutableCandleSeries {

        val candlesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            from = range.start,
            to = range.endInclusive,
        )

        return when (candlesResult) {
            is Ok -> MutableCandleSeries(candlesResult.value, timeframe)
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> {

                    if (retryOnLogin) {
                        val loginSuccessful = onCandleDataLogin()
                        if (loginSuccessful) return getCandleSeries(symbol, timeframe, range, false)
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

    private fun tabTitle(
        ticker: String,
        timeframe: Timeframe,
    ): String = "$ticker (${timeframe.toLabel()})"

    private data class ChartSession(
        val tabId: Int,
        val ticker: String,
        val timeframe: Timeframe,
        val stockChart: StockChart,
    )
}
