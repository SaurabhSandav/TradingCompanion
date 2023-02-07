package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.CrosshairMode
import com.saurabhsandav.core.chart.options.CrosshairOptions
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.charts.model.ChartsEvent
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeSymbol
import com.saurabhsandav.core.ui.charts.model.ChartsEvent.ChangeTimeframe
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.*
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.arrangement.ChartArrangement
import com.saurabhsandav.core.ui.common.chart.arrangement.paged
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApi,
) {

    private val events = MutableSharedFlow<ChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val initialSymbol = NIFTY50.first()
    private val initialTimeframe = Timeframe.M5
    private val pagedChartArrangement = ChartArrangement.paged()
    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var selectedChartManager: ChartManager? = null
    private val chartManagers = mutableListOf<ChartManager>()

    private val tabsState: StockChartTabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )
    private var chartInfo by mutableStateOf(ChartInfo(initialSymbol, initialTimeframe))
    private var legendValues by mutableStateOf(LegendValues())
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
            chartInfo = chartInfo.copy(legendValues = legendValues),
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

        // Create new chart manager
        val chartManager = when (selectedChartManager) {
            // First chart, create chart manager with initial params
            null -> {

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(initialSymbol, initialTimeframe))

                ChartManager(
                    initialParams = ChartManager.ChartParams(
                        tabId = tabId,
                        symbol = initialSymbol,
                        timeframe = initialTimeframe,
                    ),
                    actualChart = actualChart,
                    appModule = appModule,
                    onCandleDataLogin = ::onCandleDataLogin,
                )
            }
            // Copy currently selected chart manager
            else -> {

                // Currently selected chart manager
                val chartManager = requireNotNull(selectedChartManager)

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(chartManager.params.symbol, chartManager.params.timeframe))

                // Create new chart manager with existing params
                chartManager.withNewChart(
                    tabId = tabId,
                    actualChart = actualChart,
                )
            }
        }

        // Connect chart to web page
        chartPageState.connect(
            chart = chartManager.chart.actualChart,
            syncConfig = ChartPageState.SyncConfig(
                isChartFocused = { selectedChartManager == chartManager },
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

        // Switch to new tab/chart
        selectChart(tabId)
    }

    private fun closeChart(tabId: Int) {

        // Find chart manager associated with tab
        val chartManager = findChartManager(tabId)

        // Remove chart manager from cache
        chartManagers.remove(chartManager)

        // Remove chart page
        pagedChartArrangement.removeChart(chartManager.chart.actualChart)

        // Disconnect chart from web page
        chartPageState.disconnect(chartManager.chart.actualChart)

        // Cancel ChartManager CoroutineScope
        chartManager.coroutineScope.cancel()
    }

    private fun selectChart(tabId: Int) {

        // Find chart manager associated with tab
        val chartManager = findChartManager(tabId)

        // Update selected chart manager
        selectedChartManager = chartManager

        // Display newly selected chart info
        chartInfo = ChartInfo(
            symbol = chartManager.params.symbol,
            timeframe = chartManager.params.timeframe,
        )

        // Show selected chart
        pagedChartArrangement.showChart(chartManager.chart.actualChart)
    }

    private fun onChangeSymbol(symbol: String) {

        // Currently selected chart manager
        val chartManager = requireNotNull(selectedChartManager)

        // Update chart manager
        chartManager.changeSymbol(symbol)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        tabsState.setTitle(chartManager.params.tabId, tabTitle(symbol, chartManager.params.timeframe))
    }

    private fun onChangeTimeframe(timeframe: Timeframe) {

        // Currently selected chart manager
        val chartManager = requireNotNull(selectedChartManager)

        // Update chart manager
        chartManager.changeTimeframe(timeframe)

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe)

        // Update tab title
        tabsState.setTitle(chartManager.params.tabId, tabTitle(chartManager.params.symbol, timeframe))
    }

    private fun findChartManager(tabId: Int): ChartManager {
        return chartManagers.find { it.params.tabId == tabId }.let(::requireNotNull)
    }

    private fun tabTitle(
        ticker: String,
        timeframe: Timeframe,
    ): String = "$ticker (${timeframe.toLabel()})"

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
