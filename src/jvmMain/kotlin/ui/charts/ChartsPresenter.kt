package ui.charts

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import chart.options.ChartOptions
import chart.options.CrosshairMode
import chart.options.CrosshairOptions
import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import launchUnit
import trading.Timeframe
import ui.charts.model.ChartsEvent
import ui.charts.model.ChartsEvent.*
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.*
import ui.common.CollectEffect
import ui.common.UIErrorMessage
import ui.common.chart.state.TabbedChartState
import ui.common.timeframeFromLabel
import ui.common.toLabel
import ui.fyerslogin.FyersLoginState
import utils.NIFTY50
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialSymbol = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val tabbedChartState = TabbedChartState(coroutineScope)
    private val chartOptions = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
    private var maxChartId = 0
    private var currentChartId = 0
    private val chartManagers = mutableListOf<ChartManager>()

    private var tabsState by mutableStateOf(TabsState(emptyList(), 0))
    private var chartInfo by mutableStateOf(ChartInfo(initialSymbol, initialTimeframe.toLabel()))
    private var legendValues by mutableStateOf(LegendValues())
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is NewChart -> onNewChart()
                is CloseChart -> onCloseChart(event.id)
                is SelectChart -> onSelectChart(event.id)
                NextChart -> onNextChart()
                PreviousChart -> onPreviousChart()
                is ChangeSymbol -> onChangeSymbol(event.newSymbol)
                is ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ChartsState(
            tabsState = tabsState,
            chartState = tabbedChartState,
            chartInfo = chartInfo.copy(legendValues = legendValues),
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {

            // New unique id
            val id = 0

            // Add new chart
            val actualChart = tabbedChartState.addChart("Chart$id", chartOptions)

            // Create new chart manager with initial params
            val chartManager = ChartManager(
                initialParams = ChartManager.ChartParams(
                    id = id,
                    symbol = initialSymbol,
                    timeframe = initialTimeframe,
                ),
                actualChart = actualChart,
                appModule = appModule,
                onCandleDataLogin = ::onCandleDataLogin,
            )

            // Observe legend values
            chartManager.chart.legendValues.onEach { legendValues = it }.launchIn(chartManager.coroutineScope)

            // Cache newly created chart manager
            chartManagers += chartManager

            // Show Chart
            tabbedChartState.showChart(chartManager.chart.actualChart)

            // Add new tab
            updateChartTabs()
        }
    }

    private fun onNewChart() = coroutineScope.launchUnit {

        // New unique id
        val id = ++maxChartId

        // Add new chart
        val actualChart = tabbedChartState.addChart("Chart$id", chartOptions)

        // Copy currently selected chart manager
        val chartManager = run {

            // Find chart manager associated with current chart
            val chartManager = findChartManager(currentChartId)

            // Create new chart manager with existing params
            chartManager.withNewChart(
                id = id,
                actualChart = actualChart,
            )
        }

        // Observe legend values
        chartManager.chart.legendValues.onEach { legendValues = it }.launchIn(chartManager.coroutineScope)

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
        tabbedChartState.removeChart(chartManager.chart.actualChart)

        // Cancel ChartManager CoroutineScope
        chartManager.coroutineScope.cancel()

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
            symbol = chartManager.params.symbol,
            timeframe = chartManager.params.timeframe.toLabel(),
        )

        // Update tab selection
        tabsState = tabsState.copy(selectedTabIndex = chartManagerIndex)

        // Show selected chart
        tabbedChartState.showChart(chartManager.chart.actualChart)
    }

    private fun onNextChart() {

        val tabs = tabsState.tabs
        val selectedTabIndex = tabsState.selectedTabIndex
        tabs[selectedTabIndex]

        val nextChartId = when (selectedTabIndex) {
            tabs.lastIndex -> tabs.first().id
            else -> tabs[selectedTabIndex + 1].id
        }

        onSelectChart(nextChartId)
    }

    private fun onPreviousChart() {

        val tabs = tabsState.tabs
        val selectedTabIndex = tabsState.selectedTabIndex
        tabs[selectedTabIndex]

        val previousChartId = when (selectedTabIndex) {
            0 -> tabs.last().id
            else -> tabs[selectedTabIndex - 1].id
        }

        onSelectChart(previousChartId)
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Find chart manager associated with current chart
        val chartManager = findChartManager(currentChartId)

        // Update chart manager
        chartManager.changeSymbol(symbol)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        updateChartTabs()
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        val timeframe = timeframeFromLabel(newTimeframe)

        // Find chart manager associated with current chart
        val chartManager = findChartManager(currentChartId)

        // Update chart manager
        chartManager.changeTimeframe(timeframe)

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe.toLabel())

        // Update tab title
        updateChartTabs()
    }

    private fun updateChartTabs() {

        val newTabs = chartManagers.map {
            TabsState.TabInfo(
                id = it.params.id,
                title = "${it.params.symbol} (${it.params.timeframe.toLabel()})",
            )
        }

        tabsState = tabsState.copy(tabs = newTabs)
    }

    private fun findChartManager(chartId: Int): ChartManager {
        return chartManagers.find { it.params.id == chartId }.let(::requireNotNull)
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
