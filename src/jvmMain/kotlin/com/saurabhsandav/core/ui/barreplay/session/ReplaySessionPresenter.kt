package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.backtest.OrderExecution.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeOrderMarker
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Stable
internal class ReplaySessionPresenter(
    private val coroutineScope: CoroutineScope,
    private val replayParams: ReplayParams,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = appModule.candleRepo,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val events = MutableSharedFlow<ReplaySessionEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = replayParams.baseTimeframe,
        candleUpdateType = if (replayParams.replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private var autoNextJob: Job? = null
    private val stockCharts = mutableListOf<StockChart>()
    private val candleSources = mutableMapOf<StockChart.Params, ReplayCandleSource>()
    private val chartsState = StockChartsState(
        onNewChart = ::onNewChart,
        onCloseChart = ::onCloseChart,
        onChangeTicker = ::onChangeTicker,
        onChangeTimeframe = ::onChangeTimeframe,
        appModule = appModule,
    )
    private var orderFormParams by mutableStateOf(persistentListOf<OrderFormParams>())

    val replayOrdersManager = ReplayOrdersManager(coroutineScope, replayParams, barReplay, appModule)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                ResetReplay -> onResetReplay()
                AdvanceReplay -> onAdvanceReplay()
                is SetIsAutoNextEnabled -> onSetIsAutoNextEnabled(event.isAutoNextEnabled)
                is SelectProfile -> onSelectProfile(event.id)
                is Buy -> onBuy(event.stockChart)
                is Sell -> onSell(event.stockChart)
                is CancelOrder -> onCancelOrder(event.id)
                is CloseOrderForm -> onCloseOrderForm(event.id)
            }
        }

        return@launchMolecule ReplaySessionState(
            chartsState = chartsState,
            selectedProfileId = getSelectedProfileId(),
            replayOrderItems = getReplayOrderItems().value,
            orderFormParams = orderFormParams,
            chartInfo = ::getChartInfo,
        )
    }

    fun event(event: ReplaySessionEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getSelectedProfileId(): Long? {
        return remember {
            appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
                .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
                .map { it?.id }
        }.collectAsState(null).value
    }

    @Composable
    private fun getReplayOrderItems(): State<ImmutableList<ReplayOrderListItem>> {
        return remember {
            replayOrdersManager.openOrders.map { openOrders ->
                openOrders.map { openOrder ->

                    val params = openOrder.params
                    val quantity = params.quantity

                    ReplayOrderListItem(
                        id = openOrder.id,
                        execution = when (openOrder.execution) {
                            is Limit -> "Limit"
                            is Market -> "Market"
                            is StopLimit -> "Stop Limit"
                            is StopMarket -> "Stop Market"
                            is TrailingStop -> "Trailing Stop"
                        },
                        broker = run {
                            val instrumentCapitalized = params.instrument.strValue
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            "${params.broker} ($instrumentCapitalized)"
                        },
                        ticker = params.ticker,
                        quantity = params.lots
                            ?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity.toString(),
                        type = params.type.strValue.uppercase(),
                        price = when (openOrder.execution) {
                            is Limit -> openOrder.execution.price.toPlainString()
                            is Market -> ""
                            is StopLimit -> openOrder.execution.limitPrice.toPlainString()
                            is StopMarket -> openOrder.execution.trigger.toPlainString()
                            is TrailingStop -> openOrder.execution.trailingStop.toPlainString()
                        },
                        timestamp = openOrder.createdAt.time.toString(),
                    )
                }.toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    private fun onResetReplay() {
        onSetIsAutoNextEnabled(false)
        barReplay.reset()
        coroutineScope.launch {
            stockCharts.forEach { stockChart -> stockChart.newParams() }
        }
    }

    private fun onAdvanceReplay() {
        barReplay.advance()
    }

    private fun onSetIsAutoNextEnabled(isAutoNextEnabled: Boolean) {

        autoNextJob = when {
            isAutoNextEnabled -> coroutineScope.launch {
                while (isActive) {
                    delay(1.seconds)
                    barReplay.advance()
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
            ticker = prevStockChart?.currentParams?.ticker ?: replayParams.initialTicker,
            timeframe = prevStockChart?.currentParams?.timeframe ?: replayParams.baseTimeframe,
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

    private fun onSelectProfile(id: Long) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.ReplayTradingProfile, id)

        // Close all child windows
        orderFormParams = orderFormParams.clear()
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySeries = replayCandleSource.replaySeries.await()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            initialFormModel = { formValidator ->
                ReplayOrderFormModel(
                    validator = formValidator,
                    instrument = Instrument.Equity.strValue,
                    ticker = replayCandleSource.params.ticker,
                    quantity = "",
                    lots = "",
                    isBuy = true,
                    price = replaySeries.last().close.toPlainString(),
                    stop = "",
                    target = "",
                )
            },
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onSell(stockChart: StockChart) = coroutineScope.launchUnit {

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySeries = replayCandleSource.replaySeries.await()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            initialFormModel = { formValidator ->
                ReplayOrderFormModel(
                    validator = formValidator,
                    instrument = Instrument.Equity.strValue,
                    ticker = replayCandleSource.params.ticker,
                    quantity = "",
                    lots = "",
                    isBuy = false,
                    price = replaySeries.last().close.toPlainString(),
                    stop = "",
                    target = "",
                )
            },
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onCancelOrder(id: Long) {
        replayOrdersManager.cancelOrder(id)
    }

    private fun onCloseOrderForm(id: UUID) {

        val params = orderFormParams.first { it.id == id }

        orderFormParams = orderFormParams.remove(params)
    }

    private fun getChartInfo(stockChart: StockChart): ReplayChartInfo {

        val replaySeries = (stockChart.source as ReplayCandleSource?).let(::requireNotNull).replaySeries

        return ReplayChartInfo(
            replayTime = flow { emitAll(replaySeries.await().replayTime.map(::formattedReplayTime)) }
        )
    }

    private fun StockChart.newParams(
        ticker: String? = currentParams?.ticker,
        timeframe: Timeframe? = currentParams?.timeframe,
    ) {

        check(ticker != null && timeframe != null) {
            "Ticker ($ticker) and/or Timeframe ($timeframe) cannot be null"
        }

        val params = StockChart.Params(ticker, timeframe)

        val candleSource = candleSources.getOrPut(params) {
            ReplayCandleSource(
                params = params,
                replaySeriesFactory = { buildReplaySeries(params) },
                getMarkers = { candleSeries -> getMarkers(ticker, candleSeries) },
            )
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
        val unusedCandleSources = candleSources.filter { (_, candleSource) -> candleSource !in usedCandleSources }

        // Remove unused CandleSource from cache
        unusedCandleSources.forEach { (params, candleSource) ->

            // Remove from cache
            candleSources.remove(params)

            // Remove ReplaySeries from BarReplay
            barReplay.removeSeries(candleSource.replaySeries.await())
        }
    }

    private suspend fun buildReplaySeries(params: StockChart.Params): ReplaySeries {

        val candleSeries = getCandleSeries(params.ticker, replayParams.baseTimeframe)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayParams.replayFrom },
            timeframeSeries = when (replayParams.baseTimeframe) {
                params.timeframe -> null
                else -> getCandleSeries(params.ticker, params.timeframe)
            },
        )
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
                    at = replayParams.replayFrom,
                    before = replayParams.candlesBefore,
                    after = 0,
                ).bind()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayParams.replayFrom,
                    to = replayParams.dataTo,
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

    private fun getMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val replayProfile = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        val orderMarkers = replayProfile.flatMapLatest { profile ->

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            candleSeries.instantRange.flatMapLatest test@{ instantRange ->

                instantRange ?: return@test emptyFlow()

                val ldtRange = instantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                        instantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

                tradingRecord.orders.getOrdersByTickerInInterval(
                    ticker,
                    ldtRange,
                )
            }
        }
            .mapList { order ->

                val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

                TradeOrderMarker(
                    instant = orderInstant.markerTime(),
                    orderType = order.type,
                    price = order.price,
                )
            }

        val tradeMarkers = replayProfile.flatMapLatest { profile ->

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            candleSeries.instantRange.flatMapLatest test@{ instantRange ->

                instantRange ?: return@test emptyFlow()

                val ldtRange = instantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                        instantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

                tradingRecord.trades.getByTickerInInterval(ticker, ldtRange)
            }
        }
            .map { trades ->
                trades.flatMap { trade ->

                    val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())

                    buildList {

                        add(
                            TradeMarker(
                                instant = entryInstant.markerTime(),
                                isEntry = true,
                            )
                        )

                        if (trade.isClosed) {

                            val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

                            add(
                                TradeMarker(
                                    instant = exitInstant.markerTime(),
                                    isEntry = false,
                                )
                            )
                        }
                    }
                }
            }

        return orderMarkers.combine(tradeMarkers) { orderMkrs, tradeMkrs -> orderMkrs + tradeMkrs }
            .flowOn(Dispatchers.IO)
    }

    private fun formattedReplayTime(currentInstant: Instant): String {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        return DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
