package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trading.backtest.OrderExecution.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.CandleUpdateType
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Stable
internal class ReplaySessionPresenter(
    private val coroutineScope: CoroutineScope,
    replayParams: ReplayParams,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val events = MutableSharedFlow<ReplaySessionEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val barReplay = BarReplay(
        timeframe = replayParams.baseTimeframe,
        candleUpdateType = if (replayParams.replayFullBar) CandleUpdateType.FullBar else CandleUpdateType.OHLC,
    )
    private var autoNextJob: Job? = null
    private val chartsState = StockChartsState(
        initialParams = StockChartParams(replayParams.initialTicker, replayParams.baseTimeframe),
        marketDataProvider = ReplayChartsMarketDataProvider(
            coroutineScope = coroutineScope,
            replayParams = replayParams,
            barReplay = barReplay,
            appModule = appModule,
        ),
        appModule = appModule,
    )
    private var orderFormParams by mutableStateOf(persistentListOf<OrderFormParams>())

    val replayOrdersManager = ReplayOrdersManager(coroutineScope, replayParams, barReplay, appModule)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

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

        // Disable auto next
        onSetIsAutoNextEnabled(false)

        // Reset bar replay
        barReplay.reset()

        // Reset candles to initial state
        coroutineScope.launch {
            chartsState.charts.forEach { stockChart -> stockChart.refresh() }
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

    private fun onSelectProfile(id: Long) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.ReplayTradingProfile, id)

        // Close all child windows
        orderFormParams = orderFormParams.clear()
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val replayCandleSource = (stockChart.data.source as ReplayCandleSource?).let(::requireNotNull)
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

        val replayCandleSource = (stockChart.data.source as ReplayCandleSource?).let(::requireNotNull)
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

        val replaySeries = (stockChart.data.source as ReplayCandleSource?).let(::requireNotNull).replaySeries

        return ReplayChartInfo(
            replayTime = flow { emitAll(replaySeries.await().replayTime.map(::formattedReplayTime)) }
        )
    }

    private fun formattedReplayTime(currentInstant: Instant): String {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        return DateTimeFormatter.ofPattern("d MMMM, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
