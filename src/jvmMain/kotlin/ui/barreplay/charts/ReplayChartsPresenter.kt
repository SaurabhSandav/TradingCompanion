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
import launchUnit
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.barreplay.BarReplaySession
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.*
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.ui.ReplayChart
import ui.common.CollectEffect
import ui.common.chart.state.TabbedChartState
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
    private val initialSymbol: String,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay()
    private val tabbedChartState = TabbedChartState(coroutineScope)
    private val chartOptions = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
    private var autoNextJob: Job? = null
    private var maxChartId = 0
    private var currentChartId = 0

    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val dataManagers = mutableListOf<ReplayDataManager>()
    private var chartTabsState by mutableStateOf(ReplayChartTabsState(emptyList(), 0))
    private var chartInfo by mutableStateOf(ReplayChartInfo(initialSymbol, baseTimeframe.toText()))
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
            chartInfo = chartInfo.copy(legendValues = legendValues),
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            // Add new chart
            val chart = tabbedChartState.addChart("Chart$currentChartId", chartOptions)

            // Create new data manager with initial params
            val dataManager = createReplayDataManager(
                chartId = currentChartId,
                symbol = initialSymbol,
                timeframe = baseTimeframe,
                chart = ReplayChart(coroutineScope, appPrefs, chart) { legendValues = it },
            )

            // Cache newly created data manager
            dataManagers += dataManager

            // Show Chart
            tabbedChartState.showChart(dataManager.chart.chart)

            // Add new tab
            updateChartTabs()
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

        // New unique id
        val id = ++maxChartId

        // Add new chart
        val chart = tabbedChartState.addChart("Chart$id", chartOptions)

        // Copy currently selected data manager
        val dataManager = run {

            // Find data manager associated with current chart
            val dataManager = findReplayDataManager(currentChartId)

            // Create new data manager with existing params
            createReplayDataManager(
                chartId = id,
                symbol = dataManager.symbol,
                timeframe = dataManager.timeframe,
                chart = ReplayChart(coroutineScope, appPrefs, chart) { legendValues = it },
            )
        }

        // Cache newly created data manager
        dataManagers += dataManager

        // Add new tab
        updateChartTabs()

        // Switch to new tab/chart
        onSelectChart(id)
    }

    private fun onCloseChart(id: Int) {

        // Find data manager associated with chart
        val dataManager = findReplayDataManager(id)

        // Hold currently selected data manager
        val currentSelection = dataManagers[chartTabsState.selectedTabIndex]

        // Remove data manager from cache
        dataManagers.remove(dataManager)

        // Remove chart
        tabbedChartState.removeChart(dataManager.chart.chart)

        // Remove chart tab
        updateChartTabs()

        // Index of currently selected chart might've changed, change tab state accordingly
        val newSelectionIndex = dataManagers.indexOf(currentSelection)
        chartTabsState = chartTabsState.copy(selectedTabIndex = newSelectionIndex)
    }

    private fun onSelectChart(id: Int) {

        // Find data manager and index associated with current chart
        val dataManager = findReplayDataManager(id)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Update current chart id
        currentChartId = id

        // Display newly selected chart info
        chartInfo = ReplayChartInfo(
            symbol = dataManager.symbol,
            timeframe = dataManager.timeframe.toText(),
        )

        // Update tab selection
        chartTabsState = chartTabsState.copy(selectedTabIndex = dataManagerIndex)

        // Show selected chart
        tabbedChartState.showChart(dataManager.chart.chart)
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Find data manager and index associated with current chart
        val dataManager = findReplayDataManager(currentChartId)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Remove session from BarReplay
        barReplay.removeSession(dataManager.replaySession)

        // Create new data manager with new data
        val newDataManager = createReplayDataManager(
            chartId = currentChartId,
            symbol = symbol,
            timeframe = dataManager.timeframe,
            // Keep chart but reset data
            chart = dataManager.chart,
        )

        // Replace previous data manager with new data manager at same location
        dataManagers.removeAt(dataManagerIndex)
        dataManagers.add(dataManagerIndex, newDataManager)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        updateChartTabs()
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        val timeframe = when (newTimeframe) {
            "1D" -> Timeframe.D1
            else -> Timeframe.M5
        }

        // Find data manager and index associated with current chart
        val dataManager = findReplayDataManager(currentChartId)
        val dataManagerIndex = dataManagers.indexOf(dataManager)

        // Stop watching live candles
        dataManager.unsubscribeLiveCandles()

        // Create new resampled data manager
        val newDataManager = dataManager.copy(timeframe = timeframe)

        // Replace previous data manager with new data manager at same location
        dataManagers.removeAt(dataManagerIndex)
        dataManagers.add(dataManagerIndex, newDataManager)

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe.toText())

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

    private fun findReplayDataManager(chartId: Int): ReplayDataManager {
        return dataManagers.find { it.chartId == chartId }.let(::requireNotNull)
    }
}
