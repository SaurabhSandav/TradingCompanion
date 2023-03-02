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
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    private val candleCache = mutableMapOf<String, CandleSeries>()

    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var selectedChartSession: ReplayChartSession? = null
    private val chartSessions = mutableListOf<ReplayChartSession>()
    private var chartInfo by mutableStateOf(ReplayChartInfo())
    private val tabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )

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
            chartInfo = chartInfo,
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        coroutineScope.launch {
            chartSessions.forEach { chartSession -> chartSession.newParams() }
        }
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
        val chartSession = ReplayChartSession(
            tabId = tabId,
            stockChart = stockChart,
            replaySessionBuilder = ::createReplaySession,
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
            timeframe = selectedChartSession?.stockChart?.currentParams?.timeframe ?: baseTimeframe,
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

        // Update selected chart session
        selectedChartSession = chartSession

        // Display newly selected chart info
        chartInfo = ReplayChartInfo(
            stockChart = chartSession.stockChart,
            replayTime = flow {
                emitAll(chartSession.getReplaySession().replayTime.map(::formattedReplayTime))
            }
        )

        // Show selected chart
        pagedChartArrangement.showChart(chartSession.stockChart.actualChart)
    }

    private fun onChangeTicker(ticker: String) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)

        // Remove previous replay session from BarReplay
        barReplay.removeSession(chartSession.getReplaySession())

        // New chart params
        chartSession.newParams(ticker = ticker)

        // Update chart info
        chartInfo = chartInfo.copy(
            replayTime = chartSession.getReplaySession().replayTime.map(::formattedReplayTime),
        )
    }

    private fun onChangeTimeframe(timeframe: Timeframe) = coroutineScope.launchUnit {

        // Currently selected chart session
        val chartSession = requireNotNull(selectedChartSession)

        // Remove previous replay session from BarReplay
        barReplay.removeSession(chartSession.getReplaySession())

        // New chart params
        chartSession.newParams(timeframe = timeframe)

        // Update chart info
        chartInfo = chartInfo.copy(
            replayTime = chartSession.getReplaySession().replayTime.map(::formattedReplayTime)
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

    private fun formattedReplayTime(currentInstant: Instant): String {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        return DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
