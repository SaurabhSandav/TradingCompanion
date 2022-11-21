package ui.barreplay.charts

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import launchUnit
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.barreplay.BarReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.ReplayChartState
import ui.barreplay.charts.model.ReplayChartTabsState
import ui.barreplay.charts.model.ReplayChartsEvent
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.model.ReplayChartsState
import ui.barreplay.charts.ui.ReplayChart
import ui.common.CollectEffect
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
    private val initialSymbol: String,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay()
    private var autoNextJob: Job? = null
    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val dataManagers = mutableListOf<ReplayDataManager>()

    private var chartTabsState by mutableStateOf(ReplayChartTabsState(emptyList(), 0))
    private var chartState by mutableStateOf<ReplayChartState?>(null)
    private var chartData = mutableStateListOf<Pair<String, String>>()

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
            chartState = chartState,
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            // Create new session with initial params
            val dataManager = createReplayDataManager(
                chartId = 0,
                symbol = initialSymbol,
                timeframe = baseTimeframe,
                chart = ReplayChart(coroutineScope) {
                    chartData.clear()
                    chartData.addAll(it)
                },
            )

            // Cache newly created session
            dataManagers += dataManager

            // Add newly created chart into chart tabs
            updateChartTabs()

            // Display chart
            chartState = dataManager.createReplayChartState()
        }
    }

    private fun onReset() {
        onChangeIsAutoNextEnabled(false)
        barReplay.reset()
        dataManagers.forEach { it.reset() }
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

        // Currently selected chart
        val chartState = requireNotNull(chartState)

        // Copy currently selected chart session
        val dataManager = run {

            // Find session associated with current chart
            val dataManager = findReplayDataManager(chartState.id)

            // Create new session with existing params
            createReplayDataManager(
                // New unique id
                chartId = dataManagers.maxOf { it.chartId } + 1,
                symbol = dataManager.symbol,
                timeframe = dataManager.timeframe,
                chart = ReplayChart(coroutineScope) {
                    chartData.clear()
                    chartData.addAll(it)
                },
            )
        }

        // Cache newly created session
        dataManagers += dataManager

        // Add newly created chart into chart tabs
        updateChartTabs()
    }

    private fun onCloseChart(id: Int) {

        // Find session associated with chart
        val dataManager = findReplayDataManager(id)

        // Hold currently selected chart
        val currentSelection = dataManagers[chartTabsState.selectedTabIndex]

        // Remove session from cache
        dataManagers.remove(dataManager)

        // Remove chart from chart tabs
        updateChartTabs()

        // Index of currently selected chart might've changed, change tab state accordingly
        val newSelectionIndex = dataManagers.indexOf(currentSelection)
        chartTabsState = chartTabsState.copy(selectedTabIndex = newSelectionIndex)
    }

    private fun onSelectChart(id: Int) {

        // Find session and index associated with chart
        val dataManager = findReplayDataManager(id)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Display newly selected chart
        chartState = dataManager.createReplayChartState()

        // Update tab selection index
        chartTabsState = chartTabsState.copy(selectedTabIndex = dataManagerIndex)
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Currently selected chart
        val chartState = requireNotNull(chartState)

        // Find session and index associated with current chart
        val dataManager = findReplayDataManager(chartState.id)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Remove session from BarReplay
        barReplay.removeSession(dataManager.replaySession)

        // Remove session from cache
        dataManagers.removeAt(dataManagerIndex)

        // Create new session with new data
        val newDataManager = createReplayDataManager(
            chartId = chartState.id,
            symbol = symbol,
            timeframe = dataManager.timeframe,
            // Keep chart but reset data
            chart = dataManager.chart,
        )

        // Cache newly created session at same index
        dataManagers.add(dataManagerIndex, newDataManager)

        // Update tab title
        updateChartTabs()
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        // Currently selected chart
        val chartState = requireNotNull(chartState)

        val timeframe = when (newTimeframe) {
            "1D" -> Timeframe.D1
            else -> Timeframe.M5
        }

        // Find session and index associated with current chart
        val dataManager = findReplayDataManager(chartState.id)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Remove session from cache
        dataManagers.removeAt(dataManagerIndex)

        // Create new session with existing data but a different timeframe
        val newDataManager = dataManager.copy(timeframe = timeframe)

        // Cache newly created session at same index
        dataManagers.add(dataManagerIndex, newDataManager)

        // Update tab title
        updateChartTabs()
    }

    private suspend fun createReplayDataManager(
        chartId: Int,
        symbol: String,
        timeframe: Timeframe,
        chart: ReplayChart,
    ): ReplayDataManager {

        val candleSeries = getCandleSeries(symbol)

        val replaySession = barReplay.newSession { currentOffset ->

            val replayFrom = replayFrom

            BarReplaySession(
                inputSeries = candleSeries,
                initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
                currentOffset = currentOffset,
                isSessionStart = ::dailySessionStart,
            )
        }

        return ReplayDataManager(
            chartId = chartId,
            replaySession = replaySession,
            symbol = symbol,
            timeframe = timeframe,
            chart = chart,
        )
    }

    private suspend fun getCandleSeries(symbol: String): CandleSeries = candleCache.getOrPut(symbol) {

        val candleSeriesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = baseTimeframe,
            from = dataFrom,
            to = dataTo,
        )

        when (candleSeriesResult) {
            is Ok -> candleSeriesResult.value
            is Err -> when (val error = candleSeriesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun Timeframe.toText(): String = when (this) {
        Timeframe.M1 -> "1M"
        Timeframe.M5 -> "5M"
        Timeframe.D1 -> "1D"
    }

    private fun updateChartTabs() {

        val newTabs = dataManagers.map {
            ReplayChartTabsState.TabInfo(
                id = it.chartId,
                title = "${it.symbol} (${it.timeframe.toText()})",
            )
        }

        chartTabsState = chartTabsState.copy(tabs = newTabs)
    }

    private fun ReplayDataManager.createReplayChartState() = ReplayChartState(
        id = chartId,
        symbol = symbol,
        timeframe = timeframe.toText(),
        state = chart.chartState,
        data = chartData,
    ).also { chartData.clear() }

    private fun findReplayDataManager(chartId: Int): ReplayDataManager {
        return dataManagers.find { it.chartId == chartId }.let(::requireNotNull)
    }
}
