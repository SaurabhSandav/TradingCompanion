package ui.charts

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import launchUnit
import trading.CandleSeries
import trading.MutableCandleSeries
import trading.Timeframe
import trading.data.CandleRepository
import ui.charts.model.ChartsEvent
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.*
import ui.charts.ui.Chart
import ui.common.CollectEffect
import ui.common.chart.state.TabbedChartState
import ui.common.timeframeFromLabel
import ui.common.toLabel
import utils.NIFTY50
import kotlin.time.Duration.Companion.days

internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialSymbol = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val tabbedChartState = TabbedChartState(coroutineScope)
    private val chartOptions = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
    private var maxChartId = 0
    private var currentChartId = 0
    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val chartManagers = mutableListOf<ChartManager>()

    private var tabsState by mutableStateOf(TabsState(emptyList(), 0))
    private var chartInfo by mutableStateOf(ChartInfo(initialSymbol, initialTimeframe.toLabel()))
    private var legendValues by mutableStateOf(LegendValues())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is ChartsEvent.NewChart -> onNewChart()
                is ChartsEvent.CloseChart -> onCloseChart(event.id)
                is ChartsEvent.SelectChart -> onSelectChart(event.id)
                is ChartsEvent.ChangeSymbol -> onChangeSymbol(event.newSymbol)
                is ChartsEvent.ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ChartsState(
            tabsState = tabsState,
            chartState = tabbedChartState,
            chartInfo = chartInfo.copy(legendValues = legendValues),
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            // Add new chart
            val chart = tabbedChartState.addChart("Chart$currentChartId", chartOptions)

            // Create new chart manager with initial params
            val chartManager = createChartManager(
                chartId = currentChartId,
                symbol = initialSymbol,
                timeframe = initialTimeframe,
                chart = Chart(coroutineScope, appPrefs, chart) { legendValues = it },
            )

            // Cache newly created chart manager
            chartManagers += chartManager

            // Show Chart
            tabbedChartState.showChart(chartManager.chart.chart)

            // Add new tab
            updateChartTabs()
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
            val chartManager = findChartManager(currentChartId)

            // Create new chart manager with existing params
            createChartManager(
                chartId = id,
                symbol = chartManager.symbol,
                timeframe = chartManager.timeframe,
                chart = Chart(coroutineScope, appPrefs, chart) { legendValues = it },
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
        val chartManager = findChartManager(id)

        // Hold currently selected chart manager
        val currentSelection = chartManagers[tabsState.selectedTabIndex]

        // Remove chart manager from cache
        chartManagers.remove(chartManager)

        // Remove chart
        tabbedChartState.removeChart(chartManager.chart.chart)

        // Remove chart tab
        updateChartTabs()

        // Index of currently selected chart might've changed, change tab state accordingly
        val newSelectionIndex = chartManagers.indexOf(currentSelection)
        tabsState = tabsState.copy(selectedTabIndex = newSelectionIndex)
    }

    private fun onSelectChart(id: Int) {

        // Find chart manager and index associated with current chart
        val chartManager = findChartManager(id)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Update current chart id
        currentChartId = id

        // Display newly selected chart info
        chartInfo = ChartInfo(
            symbol = chartManager.symbol,
            timeframe = chartManager.timeframe.toLabel(),
        )

        // Update tab selection
        tabsState = tabsState.copy(selectedTabIndex = chartManagerIndex)

        // Show selected chart
        tabbedChartState.showChart(chartManager.chart.chart)
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Find chart manager and index associated with current chart
        val chartManager = findChartManager(currentChartId)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Create new chart manager with new data
        val newChartManager = createChartManager(
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
        val chartManager = findChartManager(currentChartId)
        val chartManagerIndex = chartManagers.indexOf(chartManager)

        // Create new chart manager with new data
        val newChartManager = createChartManager(
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

    private suspend fun createChartManager(
        chartId: Int,
        symbol: String,
        timeframe: Timeframe,
        chart: Chart,
    ): ChartManager {

        val candleSeries = getCandleSeries(symbol, timeframe)

        return ChartManager(
            chartId = chartId,
            symbol = symbol,
            timeframe = timeframe,
            chart = chart,
            candleSeries = candleSeries,
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
    ): CandleSeries = candleCache.getOrPut(symbol) {

        val currentTime = Clock.System.now()

        val candlesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            // Starting from 3 months before current time
            from = currentTime.minus(90.days),
            to = currentTime,
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
            TabsState.TabInfo(
                id = it.chartId,
                title = "${it.symbol} (${it.timeframe.toLabel()})",
            )
        }

        tabsState = tabsState.copy(tabs = newTabs)
    }

    private fun findChartManager(chartId: Int): ChartManager {
        return chartManagers.find { it.chartId == chartId }.let(::requireNotNull)
    }
}
