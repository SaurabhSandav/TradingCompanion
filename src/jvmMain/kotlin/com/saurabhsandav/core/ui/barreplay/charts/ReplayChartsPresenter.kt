package com.saurabhsandav.core.ui.barreplay.charts

import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.saurabhsandav.core.AppModule
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
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
    private var autoNextJob: Job? = null
    private val candleCache = mutableMapOf<String, CandleSeries>()
    private val chartSessions = mutableListOf<ReplayChartSession>()
    private val chartsState: StockChartsState = StockChartsState(
        onNewChart = ::onNewChart,
        onCloseChart = ::onCloseChart,
        onChangeTicker = ::onChangeTicker,
        onChangeTimeframe = ::onChangeTimeframe,
        appModule = appModule,
    )

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
            }
        }

        return@launchMolecule ReplayChartsState(
            chartsState = chartsState,
            chartInfo = ::getChartInfo,
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

    private fun onNewChart(
        newStockChart: StockChart,
        prevStockChart: StockChart?,
    ) {

        // Create new chart session
        val chartSession = ReplayChartSession(
            stockChart = newStockChart,
            replaySessionBuilder = ::createReplaySession,
        )

        // Cache newly created chart session
        chartSessions += chartSession

        // Set chart params
        // If selected chartParams is null, this is the first chart. Initialize it with initial params.
        chartSession.newParams(
            ticker = prevStockChart?.currentParams?.ticker ?: initialTicker,
            timeframe = prevStockChart?.currentParams?.timeframe ?: baseTimeframe,
        )
    }

    private fun onCloseChart(stockChart: StockChart) {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // Remove chart session from cache
        chartSessions.remove(chartSession)

        // Destroy chart
        chartSession.stockChart.destroy()
    }

    private fun onChangeTicker(
        stockChart: StockChart,
        ticker: String,
    ) = coroutineScope.launchUnit {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // Remove previous replay session from BarReplay
        barReplay.removeSession(chartSession.replaySession.first())

        // New chart params
        chartSession.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(
        stockChart: StockChart,
        timeframe: Timeframe,
    ) = coroutineScope.launchUnit {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        // Remove previous replay session from BarReplay
        barReplay.removeSession(chartSession.replaySession.first())

        // New chart params
        chartSession.newParams(timeframe = timeframe)
    }

    private fun getChartInfo(stockChart: StockChart): ReplayChartInfo {

        // Find chart session associated with StockChart
        val chartSession = chartSessions.find { it.stockChart == stockChart }.let(::requireNotNull)

        return ReplayChartInfo(
            replayTime = chartSession.replaySession.flatMapLatest { it.replayTime.map(::formattedReplayTime) }
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
