package ui.charts

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import trading.Timeframe
import ui.charts.model.ChartsEvent
import ui.charts.model.ChartsEvent.*
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.*
import ui.common.CollectEffect
import ui.common.UIErrorMessage
import ui.common.chart.state.ChartArrangement
import ui.common.chart.state.ChartPageState
import ui.common.chart.state.paged
import ui.common.timeframeFromLabel
import ui.common.toLabel
import ui.fyerslogin.FyersLoginState
import utils.NIFTY50
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ChartsPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialSymbol = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val pagedChartArrangement = ChartArrangement.paged()
    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var maxChartId = -1
    private var currentChartId = -1
    private val chartManagers = mutableListOf<ChartManager>()

    private var tabsState by mutableStateOf(TabsState(emptyList(), 0))
    private var chartInfo by mutableStateOf(ChartInfo(initialSymbol, initialTimeframe.toLabel()))
    private var legendValues by mutableStateOf(LegendValues())
    private var fyersLoginWindowState by mutableStateOf<FyersLoginWindow>(FyersLoginWindow.Closed)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                NewChart -> onNewChart()
                MoveTabBackward -> onMoveTabBackward()
                MoveTabForward -> onMoveTabForward()
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
            chartPageState = chartPageState,
            chartInfo = chartInfo.copy(legendValues = legendValues),
            fyersLoginWindowState = fyersLoginWindowState,
            errors = errors,
        )
    }

    fun event(event: ChartsEvent) {
        events.tryEmit(event)
    }

    init {

        // Initial chart
        onNewChart()
    }

    private fun onNewChart() {

        // New unique id
        val id = ++maxChartId

        // Add new chart
        val chartName = chartName(id)
        val chartContainer = pagedChartArrangement.addPage(chartName)

        // Create new chart manager
        val chartManager = when (currentChartId) {
            // First chart, create chart manager with initial params
            -1 -> ChartManager(
                initialParams = ChartManager.ChartParams(
                    id = id,
                    symbol = initialSymbol,
                    timeframe = initialTimeframe,
                ),
                container = chartContainer.value,
                name = chartName,
                appModule = appModule,
                onCandleDataLogin = ::onCandleDataLogin,
            )
            // Copy currently selected chart manager
            else -> {
                // Find chart manager associated with current chart
                val chartManager = findChartManager(currentChartId)

                // Create new chart manager with existing params
                chartManager.withNewChart(
                    id = id,
                    container = chartContainer.value,
                    name = chartName,
                )
            }
        }

        // Connect chart to web page
        chartPageState.connect(
            chart = chartManager.chart.actualChart,
            syncConfig = ChartPageState.SyncConfig(
                isChartFocused = { currentChartId == id },
                syncChartWith = { chart ->
                    val filterChartManager = chartManagers.find { it.chart.actualChart == chart }!!
                    // Sync charts with same timeframes
                    filterChartManager.params.timeframe == chartManager.params.timeframe
                },
            )
        )

        // Observe legend values
        chartManager.chart.legendValues.onEach { legendValues = it }.launchIn(chartManager.coroutineScope)

        // Cache newly created chart manager
        chartManagers += chartManager

        // Add new tab
        updateChartTabs()

        // Switch to new tab/chart
        onSelectChart(id)
    }

    private fun onMoveTabBackward() {

        // Find chart manager associated with current chart
        val chartManager = findChartManager(currentChartId)

        val currentIndex = chartManagers.indexOf(chartManager)

        if (currentIndex != 0) {

            // Reorder chart manager
            chartManagers.removeAt(currentIndex)
            chartManagers.add(currentIndex - 1, chartManager)

            // Update tabs and selection
            updateChartTabs()
            tabsState = tabsState.copy(selectedTabIndex = currentIndex - 1)
        }
    }

    private fun onMoveTabForward() {

        // Find chart manager associated with current chart
        val chartManager = findChartManager(currentChartId)

        val currentIndex = chartManagers.indexOf(chartManager)

        if (currentIndex != chartManagers.lastIndex) {

            // Reorder chart manager
            chartManagers.removeAt(currentIndex)
            chartManagers.add(currentIndex + 1, chartManager)

            // Update tabs and selection
            updateChartTabs()
            tabsState = tabsState.copy(selectedTabIndex = currentIndex + 1)
        }
    }

    private fun onCloseChart(id: Int) {

        // Find chart manager associated with chart
        val chartManager = findChartManager(id)

        // Hold currently selected chart manager
        val currentSelection = chartManagers[tabsState.selectedTabIndex]

        // Remove chart manager from cache
        chartManagers.remove(chartManager)

        // Remove chart page
        pagedChartArrangement.removePage(chartName(id))

        // Disconnect chart from web page
        chartPageState.disconnect(chartManager.chart.actualChart)

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
        pagedChartArrangement.showPage(chartName(id))
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

    private fun onChangeSymbol(symbol: String) {

        // Find chart manager associated with current chart
        val chartManager = findChartManager(currentChartId)

        // Update chart manager
        chartManager.changeSymbol(symbol)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        updateChartTabs()
    }

    private fun onChangeTimeframe(newTimeframe: String) {

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

    private fun chartName(id: Int): String = "Chart$id"
}
