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
import com.github.michaelbull.result.coroutines.binding.binding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import launchUnit
import trading.CandleSeries
import trading.MutableCandleSeries
import trading.Timeframe
import trading.barreplay.*
import trading.dailySessionStart
import trading.data.CandleRepository
import ui.barreplay.charts.model.LegendValues
import ui.barreplay.charts.model.ReplayChartInfo
import ui.barreplay.charts.model.ReplayChartsEvent
import ui.barreplay.charts.model.ReplayChartsEvent.*
import ui.barreplay.charts.model.ReplayChartsState
import ui.common.CollectEffect
import ui.common.chart.arrangement.ChartArrangement
import ui.common.chart.arrangement.paged
import ui.common.chart.state.ChartPageState
import ui.common.timeframeFromLabel
import ui.common.toLabel
import ui.stockchart.StockChartTabsState
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val candlesBefore: Int,
    private val replayFrom: Instant,
    private val dataTo: Instant,
    replayFullBar: Boolean,
    private val initialSymbol: String,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = baseTimeframe,
        candleUpdateType = if (replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private val pagedChartArrangement = ChartArrangement.paged()
    private val chartPageState = ChartPageState(coroutineScope, pagedChartArrangement)
    private var autoNextJob: Job? = null
    private var replayTimeJob: Job = Job()
    private var selectedChartManager: ReplayChartManager? = null

    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val chartManagers = mutableListOf<ReplayChartManager>()
    private val tabsState: StockChartTabsState = StockChartTabsState(
        onNew = ::newChart,
        onSelect = ::selectChart,
        onClose = ::closeChart,
    )
    private var chartInfo by mutableStateOf(ReplayChartInfo(initialSymbol, baseTimeframe.toLabel()))
    private var replayTime by mutableStateOf("")
    private var legendValues by mutableStateOf(LegendValues())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
                is ChangeSymbol -> onChangeSymbol(event.newSymbol)
                is ChangeTimeframe -> onChangeTimeframe(event.newTimeframe)
            }
        }

        return@launchMolecule ReplayChartsState(
            tabsState = tabsState,
            chartPageState = chartPageState,
            chartInfo = chartInfo.copy(
                replayTime = replayTime,
                legendValues = legendValues,
            ),
        )
    }

    fun event(event: ReplayChartsEvent) {
        events.tryEmit(event)
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

    private fun newChart(tabId: Int) = coroutineScope.launchUnit {

        // Add new chart
        val actualChart = pagedChartArrangement.newChart(
            name = "Chart$tabId",
            options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
        )

        // Create new chart manager
        val chartManager = when (selectedChartManager) {
            // First chart, create chart manager with initial params
            null -> {

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(initialSymbol, baseTimeframe))

                ReplayChartManager(
                    initialParams = ReplayChartManager.ChartParams(
                        tabId = tabId,
                        symbol = initialSymbol,
                        timeframe = baseTimeframe,
                        // New replay session
                        replaySession = createReplaySession(initialSymbol, baseTimeframe),
                    ),
                    actualChart = actualChart,
                    appModule = appModule,
                )
            }

            // Copy currently selected chart manager
            else -> {

                // Currently selected chart manager
                val chartManager = requireNotNull(selectedChartManager)

                // Set tab title
                tabsState.setTitle(tabId, tabTitle(chartManager.params.symbol, chartManager.params.timeframe))

                // New replay session
                val replaySession = createReplaySession(
                    symbol = chartManager.params.symbol,
                    timeframe = chartManager.params.timeframe,
                )

                // Create new chart manager with existing params
                chartManager.withNewChart(
                    tabId = tabId,
                    actualChart = actualChart,
                    replaySession = replaySession,
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

        // Cancel chart manager coroutines
        chartManager.coroutineScope.cancel()

        // Remove chart manager from cache
        chartManagers.remove(chartManager)

        // Remove chart page
        pagedChartArrangement.removeChart(chartManager.chart.actualChart)

        // Disconnect chart from web page
        chartPageState.disconnect(chartManager.chart.actualChart)
    }

    private fun selectChart(tabId: Int) {

        // Find chart manager associated with tab
        val chartManager = findChartManager(tabId)

        // Update selected chart manager
        selectedChartManager = chartManager

        // Display newly selected chart info
        chartInfo = ReplayChartInfo(
            symbol = chartManager.params.symbol,
            timeframe = chartManager.params.timeframe.toLabel(),
        )

        // Show selected chart
        pagedChartArrangement.showChart(chartManager.chart.actualChart)

        // Show replay time using currently selected chart data
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            chartManager.params.replaySession.replayTime.collect(::updateTime)
        }
    }

    private fun onChangeSymbol(symbol: String) = coroutineScope.launchUnit {

        // Currently selected chart manager
        val chartManager = requireNotNull(selectedChartManager)

        // Remove session from BarReplay
        barReplay.removeSession(chartManager.params.replaySession)

        // New replay session
        val replaySession = createReplaySession(symbol, chartManager.params.timeframe)

        // Update chart manager
        chartManager.changeSymbol(symbol, replaySession)

        // Update chart info
        chartInfo = chartInfo.copy(symbol = symbol)

        // Update tab title
        tabsState.setTitle(chartManager.params.tabId, tabTitle(symbol, chartManager.params.timeframe))

        // Show replay time using new session
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            replaySession.replayTime.collect(::updateTime)
        }
    }

    private fun onChangeTimeframe(newTimeframe: String) = coroutineScope.launchUnit {

        val timeframe = timeframeFromLabel(newTimeframe)

        // Currently selected chart manager
        val chartManager = requireNotNull(selectedChartManager)

        // Remove session from BarReplay
        barReplay.removeSession(chartManager.params.replaySession)

        // New replay session
        val replaySession = createReplaySession(chartManager.params.symbol, timeframe)

        // Update chart manager
        chartManager.changeTimeframe(timeframe, replaySession)

        // Update chart info
        chartInfo = chartInfo.copy(timeframe = timeframe.toLabel())

        // Update tab title
        tabsState.setTitle(chartManager.params.tabId, tabTitle(chartManager.params.symbol, timeframe))

        // Show replay time using new session
        replayTimeJob.cancel()
        replayTimeJob = coroutineScope.launch {
            replaySession.replayTime.collect(::updateTime)
        }
    }

    private suspend fun createReplaySession(
        symbol: String,
        timeframe: Timeframe,
    ): BarReplaySession {

        val candleSeries = getCandleSeries(symbol, baseTimeframe)
        val timeframeSeries = if (baseTimeframe == timeframe) null else getCandleSeries(symbol, timeframe)

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
        symbol: String,
        timeframe: Timeframe,
    ): CandleSeries = candleCache.getOrPut("${symbol}_${timeframe.seconds}") {

        val allCandlesResult = binding {

            val candlesBefore = async {
                candleRepo.getCandles(
                    symbol = symbol,
                    timeframe = timeframe,
                    at = replayFrom,
                    before = candlesBefore,
                    after = 0,
                ).bind()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    symbol = symbol,
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

    private fun findChartManager(tabId: Int): ReplayChartManager {
        return chartManagers.find { it.params.tabId == tabId }.let(::requireNotNull)
    }

    private fun tabTitle(
        ticker: String,
        timeframe: Timeframe,
    ): String = "$ticker (${timeframe.toLabel()})"

    private fun updateTime(currentInstant: Instant) {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        replayTime = DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
