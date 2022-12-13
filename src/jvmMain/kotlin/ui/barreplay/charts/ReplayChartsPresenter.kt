package ui.barreplay.charts

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import chart.options.ChartOptions
import chart.options.CrosshairMode
import chart.options.CrosshairOptions
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import launchUnit
import trading.CandleSeries
import trading.MutableCandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.barreplay.CandleUpdateType
import trading.barreplay.ResampledBarReplaySession
import trading.barreplay.SimpleBarReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.*
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayChart
import ui.common.CollectEffect
import ui.common.chart.state.TabbedChartState
import ui.common.timeframeFromLabel
import ui.common.toLabel
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
    replayFullBar: Boolean,
    private val initialSymbol: String,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = baseTimeframe,
        candleUpdateType = if (replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private val tabbedChartState = TabbedChartState(coroutineScope)
    private val chartOptions = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
    private var autoNextJob: Job? = null
    private var maxChartId = 0
    private var currentChartId = 0
    private var replayTimeJob: Job = Job()

    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val chartManagers = mutableListOf<ReplayChartManager>()
    private var chartTabsState by mutableStateOf(ReplayChartTabsState(emptyList(), 0))
    private var chartInfo by mutableStateOf(ReplayChartInfo(initialSymbol, baseTimeframe.toLabel()))
    private var replayTime by mutableStateOf("")
    private var legendValues by mutableStateOf(LegendValues())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
                is NewChart -> onNewChart()
                is CloseChart -> onCloseChart(event.id)
                is SelectChart -> onSelectChart(event.id)
                is ChangeSymbol -> onChangeSymbol(event.newSymbol)
                is ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ReplayChartsState(
            chartTabsState = chartTabsState,
            chartState = tabbedChartState,
            chartInfo = chartInfo.copy(
                replayTime = replayTime,
                legendValues = legendValues,
            ),
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            // Add new chart
            val chart = tabbedChartState.addChart("Chart$currentChartId", chartOptions)

            // Create new chart manager with initial params
            val chartManager = createReplayChartManager(
                chartId = currentChartId,
                symbol = initialSymbol,
                timeframe = baseTimeframe,
                chart = ReplayChart(coroutineScope, appPrefs, chart) { legendValues = it },
            )

            // Cache newly created chart manager
            chartManagers += chartManager

            // Show Chart
            tabbedChartState.showChart(chartManager.chart.chart)

            // Add new tab
            updateChartTabs()

            // Show replay time using currently selected chart data
            updateTime(chartManager.replaySession.replaySeries.last().openInstant)
            replayTimeJob = coroutineScope.launch {
                chartManager.replaySession.replaySeries.live.collect { updateTime(it.openInstant) }
            }
        }
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        chartManagers.forEach { it.reset() }
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

    private fun onNewChart() = coroutineScope.launchUnit {

        // New unique id
        val id = ++maxChartId

        // Add new chart
        val chart = tabbedChartState.addChart("Chart$id", chartOptions)

        // Copy currently selected chart manager
        val chartManager = run {

            // Find chart manager associated with current chart
            val chartManager = findReplayChartManager(currentChartId)

            // Create new chart manager with existing params
            createReplayChartManager(
                chartId = id,
                symbol = chartManager.symbol,
                timeframe = chartManager.timeframe,
                chart = ReplayChart(coroutineScope, appPrefs, chart) { legendValues = it },
            )
        }

        // Cache newly created chart manager
        chartManagers += chartManager

        // Add new tab
        updateChartTabs()

        // Switch to new tab/chart
        onSelectChart(id)
    }

    private fun onCloseChart(id: Int) {

        // Find chart manager associated with chart
        val chartManager = findReplayChartManager(id)

        // Hold currently selected chart manager
        val currentSelection = chartManagers[chartTabsState.selectedTabIndex]

        // Remove chart manager from cache
        chartManagers.remove(chartManager)

        // Remove chart
        tabbedChartState.removeChart(chartManager.chart.chart)

        // Remove chart tab
        updateChartTabs()

        // Index of currently selected chart might've changed, change tab state accordingly
        val newSelectionIndex = chartManagers.indexOf(currentSelection)
        chartTabsState = chartTabsState.copy(selectedTabIndex = newSelectionIndex)
    }

    private fun onSelectChart(id: Int) {

        // Find chart manager and index associated with current chart
        val chartManager = findReplayChartManager(id)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Update current chart id
        currentChartId = id

        // Display newly selected chart info
        chartInfo = ReplayChartInfo(
            symbol = chartManager.symbol,
            timeframe = chartManager.timeframe.toLabel(),
        )

        // Update tab selection
        chartTabsState = chartTabsState.copy(selectedTabIndex = chartManagerIndex)

        // Show selected chart
        tabbedChartState.showChart(chartManager.chart.chart)

        // Show replay time using currently selected chart data
        updateTime(chartManager.replaySession.replaySeries.last().openInstant)
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            chartManager.replaySession.replaySeries.live.collect { updateTime(it.openInstant) }
        }
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Find chart manager and index associated with current chart
        val chartManager = findReplayChartManager(currentChartId)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Stop watching live candles
        chartManager.unsubscribeLiveCandles()

        // Remove session from BarReplay
        barReplay.removeSession(chartManager.replaySession)

        // Create new chart manager with new data
        val newChartManager = createReplayChartManager(
            chartId = currentChartId,
            symbol = symbol,
            timeframe = chartManager.timeframe,
            // Keep chart but reset data
            chart = chartManager.chart,
        )

        // Replace previous chart manager with new chart manager at same location
        chartManagers.removeAt(chartManagerIndex)
        chartManagers.add(chartManagerIndex, newChartManager)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        updateChartTabs()
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        val timeframe = timeframeFromLabel(newTimeframe)

        // Find chart manager and index associated with current chart
        val chartManager = findReplayChartManager(currentChartId)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Stop watching live candles
        chartManager.unsubscribeLiveCandles()

        // Remove session from BarReplay
        barReplay.removeSession(chartManager.replaySession)

        // Create new resampled chart manager
        val newChartManager = createReplayChartManager(
            chartId = currentChartId,
            symbol = chartManager.symbol,
            timeframe = timeframe,
            // Keep chart but reset data
            chart = chartManager.chart,
        )

        // Replace previous chart manager with new chart manager at same location
        chartManagers.removeAt(chartManagerIndex)
        chartManagers.add(chartManagerIndex, newChartManager)

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe.toLabel())

        // Update tab title
        updateChartTabs()
    }

    private suspend fun createReplayChartManager(
        chartId: Int,
        symbol: String,
        timeframe: Timeframe,
        chart: ReplayChart,
    ): ReplayChartManager {

        val candleSeries = getCandleSeries(symbol, baseTimeframe)
        val timeframeSeries = if (baseTimeframe == timeframe) null else getCandleSeries(symbol, timeframe)

        val replaySession = barReplay.newSession { currentOffset, currentCandleState ->

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

        return ReplayChartManager(
            chartId = chartId,
            replaySession = replaySession,
            symbol = symbol,
            timeframe = timeframe,
            chart = chart,
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
    ): CandleSeries = candleCache.getOrPut("${symbol}_${timeframe.seconds}") {

        val candlesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            from = dataFrom,
            to = dataTo,
        )

        when (candlesResult) {
            is Ok ->  MutableCandleSeries(candlesResult.value, timeframe)
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun updateChartTabs() {

        val newTabs = chartManagers.map {
            ReplayChartTabsState.TabInfo(
                id = it.chartId,
                title = "${it.symbol} (${it.timeframe.toLabel()})",
            )
        }

        chartTabsState = chartTabsState.copy(tabs = newTabs)
    }

    private fun findReplayChartManager(chartId: Int): ReplayChartManager {
        return chartManagers.find { it.chartId == chartId }.let(::requireNotNull)
    }

    private fun updateTime(currentInstant: Instant) {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        replayTime = DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
