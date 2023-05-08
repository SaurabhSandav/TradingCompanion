package com.saurabhsandav.core.ui.barreplay.charts

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
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsEvent.*
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState.OrderFormParams
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState.ReplayChartInfo
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeOrderMarker
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
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
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = appModule.candleRepo,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
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
                is SelectProfile -> onSelectProfile(event.id)
                is Buy -> onBuy(event.stockChart)
                is Sell -> onSell(event.stockChart)
                is CloseOrderForm -> onCloseOrderForm(event.id)
            }
        }

        return@launchMolecule ReplayChartsState(
            chartsState = chartsState,
            selectedProfileId = remember { appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile) }
                .collectAsState(null).value,
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
        barReplay.advance()
    }

    private fun onChangeIsAutoNextEnabled(isAutoNextEnabled: Boolean) {

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

    private fun onSelectProfile(id: Long) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.ReplayTradingProfile, id)

        // Close all child windows
        orderFormParams = orderFormParams.clear()
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val id = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile).first() ?: return@launchUnit
        val currentProfile = tradingProfiles.getProfile(id).first()

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySeries = replayCandleSource.replaySeries.await()
        val replayTime = replaySeries.replayTime.first()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = currentProfile.id,
            formType = OrderFormType.New { formValidator ->
                OrderFormModel(
                    validator = formValidator,
                    instrument = Instrument.Equity.strValue,
                    ticker = replayCandleSource.ticker,
                    quantity = "",
                    lots = "",
                    isBuy = true,
                    price = replaySeries.last().close.toPlainString(),
                    timestamp = replayTime.toLocalDateTime(TimeZone.currentSystemDefault()),
                )
            },
        )

        orderFormParams = orderFormParams.add(params)
    }

    private fun onSell(stockChart: StockChart) = coroutineScope.launchUnit {

        val id = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile).first() ?: return@launchUnit
        val currentProfile = tradingProfiles.getProfile(id).first()

        val replayCandleSource = (stockChart.source as ReplayCandleSource?).let(::requireNotNull)
        val replaySeries = replayCandleSource.replaySeries.await()
        val replayTime = replaySeries.replayTime.first()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = currentProfile.id,
            formType = OrderFormType.New { formValidator ->
                OrderFormModel(
                    validator = formValidator,
                    instrument = Instrument.Equity.strValue,
                    ticker = replayCandleSource.ticker,
                    quantity = "",
                    lots = "",
                    isBuy = false,
                    price = replaySeries.last().close.toPlainString(),
                    timestamp = replayTime.toLocalDateTime(TimeZone.currentSystemDefault()),
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

        val candleSource = candleSources.getOrPut(ticker to timeframe) {
            ReplayCandleSource(
                ticker = ticker,
                timeframe = timeframe,
                replaySeriesFactory = { buildReplaySeries(ticker, timeframe) },
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

    private suspend fun buildReplaySeries(
        ticker: String,
        timeframe: Timeframe,
    ): ReplaySeries {

        val candleSeries = getCandleSeries(ticker, baseTimeframe)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayFrom },
            timeframeSeries = if (baseTimeframe == timeframe) null else getCandleSeries(ticker, timeframe),
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

    private fun getMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val replayProfile = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfile(id) else flowOf(null) }
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
