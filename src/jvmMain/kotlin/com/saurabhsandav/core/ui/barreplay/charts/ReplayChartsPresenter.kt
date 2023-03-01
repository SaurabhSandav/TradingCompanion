package com.saurabhsandav.core.ui.barreplay.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.*
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartInfo
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent.*
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val candlesBefore: Int,
    private val replayFrom: Instant,
    private val dataTo: Instant,
    replayFullBar: Boolean,
    private val initialTicker: String,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = baseTimeframe,
        candleUpdateType = if (replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private val pagedChartArrangement = ChartArrangement.paged()
    private var autoNextJob: Job? = null
    private var replayTimeJob: Job = Job()
    private val candleCache = mutableMapOf<String, CandleSeries>()

    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var selectedChartSession: ChartSession? = null
    private val chartSessions = mutableListOf<ChartSession>()
    private val tabsState: StockChartTabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )
    private var chartInfo by mutableStateOf(ReplayChartInfo(initialTicker, baseTimeframe))
    private var replayTime by mutableStateOf("")

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
                is ChangeTicker -> onChangeTicker(event.newTicker)
                is ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ReplayChartsState(
            tabsState = tabsState,
            chartPageState = chartPageState,
            chartInfo = chartInfo.copy(replayTime = replayTime),
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        chartSessions.forEach { it.stockChart.setCandleSource(it.buildCandleSource()) }
    }

    private fun onNext() {
        barReplay.next()
    }

    private fun onChangeIsAutoNextEnabled(isAutoNextEnabled: Boolean) {

        autoNextJob = when {
            isAutoNextEnabled -> coroutineScope.launch {
                while (isActive) {
                    delay(1.seconds)
                    barReplay.next()
                }
            }

            else -> {
                autoNextJob?.cancel()
                null
            }
        }
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
            onTitleUpdate = { tabsState.setTitle(tabId, it) },
        )

        // Create new chart session
        val chartSession = when (selectedChartSession) {
            // First chart, create chart session with initial params
            null -> {

                val replaySession = createReplaySession(initialTicker, baseTimeframe)

                ChartSession(
                    tabId = tabId,
                    ticker = initialTicker,
                    timeframe = baseTimeframe,
                    // New replay session
                    replaySession = replaySession,
                    stockChart = stockChart,
                )
            }

            else -> {

                // Currently selected chart session
                val chartSession = requireNotNull(selectedChartSession)

                // New replay session
                val replaySession = createReplaySession(
                    ticker = chartSession.ticker,
                    timeframe = chartSession.timeframe,
                )

                // Create new chart session with existing params
                chartSession.copy(
                    tabId = tabId,
                    replaySession = replaySession,
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
                    val filterChartSession = chartSessions.find { it.stockChart.actualChart == chart }!!
                    // Sync charts with same timeframes
                    filterChartSession.timeframe == chartSession.timeframe
                },
            )
        )

        // Cache newly created chart session
        chartSessions += chartSession

        // Set candle source for chart
        chartSession.stockChart.setCandleSource(chartSession.buildCandleSource())

        // Switch to new chart
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

        // Update selected chart session
        selectedChartSession = chartSession

        // Display newly selected chart info
        chartInfo = ReplayChartInfo(
            ticker = chartSession.ticker,
            timeframe = chartSession.timeframe,
        )

        // Show selected chart
        pagedChartArrangement.showChart(chartSession.stockChart.actualChart)

        // Show replay time using currently selected chart data
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            chartSession.replaySession.replayTime.collect(::updateTime)
        }
    }

    private fun onChangeTicker(ticker: String) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)
        val chartSessionIndex = chartSessions.indexOf(chartSession)

        // Remove session from BarReplay
        barReplay.removeSession(chartSession.replaySession)

        // New replay session
        val replaySession = createReplaySession(ticker, chartSession.timeframe)

        // New chart session
        val newChartSession = chartSession.copy(
            ticker = ticker,
            replaySession = replaySession,
        )

        // Update chart session
        chartSessions[chartSessionIndex] = newChartSession

        // Replace chart candle source
        newChartSession.stockChart.setCandleSource(newChartSession.buildCandleSource())

        // Update chart info
        chartInfo = chartInfo.copy(ticker = ticker)

        // Show replay time using new session
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            replaySession.replayTime.collect(::updateTime)
        }
    }

    private fun onChangeTimeframe(timeframe: Timeframe) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)
        val chartSessionIndex = chartSessions.indexOf(chartSession)

        // Remove session from BarReplay
        barReplay.removeSession(chartSession.replaySession)

        // New replay session
        val replaySession = createReplaySession(chartSession.ticker, timeframe)

        // New chart session
        val newChartSession = chartSession.copy(
            timeframe = timeframe,
            replaySession = replaySession,
        )

        // Update chart session
        chartSessions[chartSessionIndex] = newChartSession

        // Replace chart candle source
        newChartSession.stockChart.setCandleSource(newChartSession.buildCandleSource())

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe)

        // Show replay time using new session
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            replaySession.replayTime.collect(::updateTime)
        }
    }

    private fun ChartSession.buildCandleSource(): CandleSource {
        return CandleSource(
            ticker = ticker,
            timeframe = timeframe,
            candleSeries = replaySession.replaySeries,
            hasVolume = ticker != "NIFTY50",
        )
    }

    private suspend fun createReplaySession(
        ticker: String,
        timeframe: Timeframe,
    ): BarReplaySession {

        val candleSeries = getCandleSeries(ticker, baseTimeframe)
        val timeframeSeries = if (baseTimeframe == timeframe) null else getCandleSeries(ticker, timeframe)

        return barReplay.newSession { currentOffset, currentCandleState ->

            when (baseTimeframe) {
                timeframe -> SimpleBarReplaySession(
                    inputSeries = candleSeries,
                    initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
                    currentOffset = currentOffset,
                    currentCandleState = currentCandleState,
                )

                else -> ResampledBarReplaySession(
                    inputSeries = candleSeries,
                    initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
                    currentOffset = currentOffset,
                    currentCandleState = currentCandleState,
                    timeframeSeries = timeframeSeries!!,
                    isSessionStart = ::dailySessionStart,
                )
            }
        }
    }

    private suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries = candleCache.getOrPut("${ticker}_${timeframe.seconds}") {

        val allCandlesResult = binding {

            val candlesBefore = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    at = replayFrom,
                    before = candlesBefore,
                    after = 0,
                ).bind()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayFrom,
                    to = dataTo,
                ).bind()
            }

            candlesBefore.await() + candlesAfter.await()
        }

        when (allCandlesResult) {
            is Ok -> MutableCandleSeries(allCandlesResult.value, timeframe)
            is Err -> when (val error = allCandlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun updateTime(currentInstant: Instant) {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        replayTime = DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }

    private data class ChartSession(
        val tabId: Int,
        val ticker: String,
        val timeframe: Timeframe,
        val replaySession: BarReplaySession,
        val stockChart: StockChart,
    )
}
