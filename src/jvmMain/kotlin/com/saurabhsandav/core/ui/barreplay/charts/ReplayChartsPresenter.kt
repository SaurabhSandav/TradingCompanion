package com.saurabhsandav.core.ui.barreplay.charts

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent.*
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState.OrderFormParams
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState.ReplayChartInfo
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.persistentListOf
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
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Stable
internal class ReplayChartsPresenter(
    private val coroutineScope: CoroutineScope,
    private val baseTimeframe: Timeframe,
    private val candlesBefore: Int,
    private val replayFrom: Instant,
    private val dataTo: Instant,
    replayFullBar: Boolean,
    private val initialTicker: String,
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = appModule.candleRepo,
) {

    private val events = MutableSharedFlow<ReplayChartsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = baseTimeframe,
        candleUpdateType = if (replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private var autoNextJob: Job? = null
    private val stockCharts = mutableListOf<StockChart>()
    private val candleSources = mutableMapOf<Pair<String, Timeframe>, ReplayCandleSource>()
    private val chartsState = StockChartsState(
        onNewChart = ::onNewChart,
        onCloseChart = ::onCloseChart,
        onChangeTicker = ::onChangeTicker,
        onChangeTimeframe = ::onChangeTimeframe,
        appModule = appModule,
    )
    private var orderFormParams by mutableStateOf(persistentListOf<OrderFormParams>())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                Reset -> onReset()
                Next -> onNext()
                is ChangeIsAutoNextEnabled -> onChangeIsAutoNextEnabled(event.isAutoNextEnabled)
                is Buy -> onBuy(event.stockChart)
                is Sell -> onSell(event.stockChart)
                is CloseOrderForm -> onCloseOrderForm(event.id)
            }
        }

        return@launchMolecule ReplayChartsState(
            chartsState = chartsState,
            orderFormParams = orderFormParams,
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
            stockCharts.forEach { stockChart -> stockChart.newParams() }
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

        // Cache StockChart
        stockCharts += newStockChart

        // Set chart params
        // If selected chartParams is null, this is the first chart. Initialize it with initial params.
        newStockChart.newParams(
            ticker = prevStockChart?.currentParams?.ticker ?: initialTicker,
            timeframe = prevStockChart?.currentParams?.timeframe ?: baseTimeframe,
        )
    }

    private fun onCloseChart(stockChart: StockChart) {

        // Remove chart session from cache
        stockCharts.remove(stockChart)

        // Destroy chart
        stockChart.destroy()

        // Remove unused ReplayCandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun onChangeTicker(stockChart: StockChart, ticker: String) {

        // New chart params
        stockChart.newParams(ticker = ticker)
    }

    private fun onChangeTimeframe(stockChart: StockChart, timeframe: Timeframe) {

        // New chart params
        stockChart.newParams(timeframe = timeframe)
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySession = replayCandleSource.replaySession.await()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            formType = OrderFormType.New { formValidator ->
                OrderFormModel(
                    validator = formValidator,
                    ticker = replayCandleSource.ticker,
                    quantity = "",
                    isBuy = true,
                    price = replaySession.replaySeries.last().close.toPlainString(),
                    timestamp = replaySession.replayTime.value.toLocalDateTime(TimeZone.currentSystemDefault()),
                )
            },
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onSell(stockChart: StockChart) = coroutineScope.launchUnit {

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySession = replayCandleSource.replaySession.await()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            formType = OrderFormType.New { formValidator ->
                OrderFormModel(
                    validator = formValidator,
                    ticker = replayCandleSource.ticker,
                    quantity = "",
                    isBuy = false,
                    price = replaySession.replaySeries.last().close.toPlainString(),
                    timestamp = replaySession.replayTime.value.toLocalDateTime(TimeZone.currentSystemDefault()),
                )
            },
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onCloseOrderForm(id: UUID) {

        val params = orderFormParams.first { it.id == id }

        orderFormParams = orderFormParams.remove(params)
    }

    private fun getChartInfo(stockChart: StockChart): ReplayChartInfo {

        val replaySession = (stockChart.source as ReplayCandleSource?).let(::requireNotNull).replaySession

        return ReplayChartInfo(
            replayTime = flow { emitAll(replaySession.await().replayTime.map(::formattedReplayTime)) }
        )
    }

    private fun StockChart.newParams(
        ticker: String? = currentParams?.ticker,
        timeframe: Timeframe? = currentParams?.timeframe,
    ) {

        check(ticker != null && timeframe != null) {
            "Ticker ($ticker) and/or Timeframe ($timeframe) cannot be null"
        }

        val candleSource = candleSources.getOrPut(ticker to timeframe) {
            ReplayCandleSource(ticker, timeframe, ::createReplaySession)
        }

        // Set ReplayCandleSource on StockChart
        setCandleSource(candleSource)

        // Remove unused ReplayCandleSources from cache
        releaseUnusedCandleSources()
    }

    private fun releaseUnusedCandleSources() = coroutineScope.launchUnit {

        // CandleSources currently in use
        val usedCandleSources = stockCharts.mapNotNull { stockChart -> stockChart.source }

        // CandleSources not in use
        val unusedCandleSources = candleSources.filter { it.value !in usedCandleSources }

        // Remove unused CandleSource from cache
        unusedCandleSources.forEach {

            // Remove from cache
            candleSources.remove(it.key)

            // Remove ReplaySession from BarReplay
            barReplay.removeSession(it.value.replaySession.await())
        }
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
    ): CandleSeries {

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

        return when (allCandlesResult) {
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
