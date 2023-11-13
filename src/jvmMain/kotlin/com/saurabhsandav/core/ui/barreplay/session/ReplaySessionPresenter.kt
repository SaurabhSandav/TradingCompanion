package com.saurabhsandav.core.ui.barreplay.session

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.backtest.OrderExecutionType.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState.ReplayParams
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionEvent.*
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.*
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.format
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Stable
internal class ReplaySessionPresenter(
    private val coroutineScope: CoroutineScope,
    replayParams: ReplayParams,
    stockChartsStateFactory: (StockChartParams) -> StockChartsState,
    private val barReplay: BarReplay,
    val replayOrdersManager: ReplayOrdersManager,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
) {

    private var autoNextJob: Job? = null
    private val chartsState = stockChartsStateFactory(
        StockChartParams(replayParams.initialTicker, replayParams.baseTimeframe)
    )
    private val orderFormWindowsManager = AppWindowsManager<OrderFormParams>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ReplaySessionState(
            chartsState = chartsState,
            selectedProfileId = getSelectedProfileId(),
            replayOrderItems = getReplayOrderItems().value,
            orderFormWindowsManager = orderFormWindowsManager,
            chartInfo = ::getChartInfo,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ReplaySessionEvent) {

        when (event) {
            ResetReplay -> onResetReplay()
            AdvanceReplay -> onAdvanceReplay()
            AdvanceReplayByBar -> onAdvanceReplayByBar()
            is SetIsAutoNextEnabled -> onSetIsAutoNextEnabled(event.isAutoNextEnabled)
            is SelectProfile -> onSelectProfile(event.id)
            is Buy -> onBuy(event.stockChart)
            is Sell -> onSell(event.stockChart)
            is CancelOrder -> onCancelOrder(event.id)
        }
    }

    @Composable
    private fun getSelectedProfileId(): ProfileId? {
        return remember {
            appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
                .map { it?.let(::ProfileId) }
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
                        executionType = when (openOrder.executionType) {
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
                        side = params.side.strValue.uppercase(),
                        price = when (openOrder.executionType) {
                            is Limit -> openOrder.executionType.price.toPlainString()
                            is Market -> ""
                            is StopLimit -> openOrder.executionType.limitPrice.toPlainString()
                            is StopMarket -> openOrder.executionType.trigger.toPlainString()
                            is TrailingStop -> openOrder.executionType.trailingStop.toPlainString()
                        },
                        timestamp = TradeDateTimeFormatter.format(
                            ldt = openOrder.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()),
                        ),
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
        chartsState.charts.forEach { stockChart -> stockChart.refresh() }
    }

    private fun onAdvanceReplay() {
        barReplay.advance()
    }

    private fun onAdvanceReplayByBar() {
        barReplay.advanceByBar()
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

    private fun onSelectProfile(id: ProfileId) = coroutineScope.launchUnit {

        // Save selected profile
        appPrefs.putLong(PrefKeys.ReplayTradingProfile, id.value)

        // Close all child windows
        orderFormWindowsManager.closeAll()
    }

    private fun onBuy(stockChart: StockChart) = coroutineScope.launchUnit {

        val price = stockChart.data
            .getCandleSeries()
            .last()
            .close
            .toPlainString()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            initialModel = ReplayOrderFormModel.Initial(
                instrument = Instrument.Equity,
                ticker = stockChart.params.ticker,
                isBuy = true,
                price = price,
            ),
        )

        orderFormWindowsManager.newWindow(params)
    }

    private fun onSell(stockChart: StockChart) = coroutineScope.launchUnit {

        val price = stockChart.data
            .getCandleSeries()
            .last()
            .close
            .toPlainString()

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            initialModel = ReplayOrderFormModel.Initial(
                instrument = Instrument.Equity,
                ticker = stockChart.params.ticker,
                isBuy = false,
                price = price,
            ),
        )

        orderFormWindowsManager.newWindow(params)
    }

    private fun onCancelOrder(id: Long) {
        replayOrdersManager.cancelOrder(id)
    }

    private fun getChartInfo(stockChart: StockChart) = ReplayChartInfo(
        replayTime = flow {

            val replayTimeFlow = stockChart.data
                .getCandleSeries()
                .let { it as ReplaySeries }
                .replayTime
                .map(::formattedReplayTime)

            emitAll(replayTimeFlow)
        },
        candleState = flow {

            val candleStateFlow = stockChart.data
                .getCandleSeries()
                .let { it as ReplaySeries }
                .candleState
                .map { it.name }

            emitAll(candleStateFlow)
        },
    )

    private fun formattedReplayTime(currentInstant: Instant): String {
        val localDateTime = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        return DateTimeFormatter.ofPattern("MMM d, yyyy\nHH:mm:ss").format(localDateTime)
    }
}
